package com.sunwayMinecraft.containerfinder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Barrel;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.ShulkerBox;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.util.BoundingBox;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Performs the actual scanning and aggregation for container searches.
 *
 * <p>This class is responsible for:
 *
 * <ul>
 *   <li>Scanning chunk tile entities inside a bounding area
 *   <li>Finding supported storage blocks
 *   <li>Deduplicating double chests
 *   <li>Summarizing direct and nested shulker contents
 *   <li>Tracking counts and building the final scan cache
 * </ul>
 */
public class ContainerScanProcessor {
    private static final DateTimeFormatter FILE_TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final World world;
    private final BoundingBox area;
    private final int radius;
    private final boolean nearFloorOnly;
    private final int scannedChunks;
    private final int skippedUnloadedChunks;

    private final Set<String> seenDoubleChests = new HashSet<>();
    private final List<ContainerRecord> nonEmptyRecords = new ArrayList<>();
    private final Map<String, Long> directTotals = new LinkedHashMap<>();
    private final Map<String, String> directLabels = new LinkedHashMap<>();
    private final Map<String, Long> nestedTotals = new LinkedHashMap<>();
    private final Map<String, String> nestedLabels = new LinkedHashMap<>();
    private final Set<String> distinctItemGroups = new HashSet<>();

    private int totalContainers = 0;
    private int chestCount = 0;
    private int trappedChestCount = 0;
    private int doubleChestCount = 0;
    private int barrelCount = 0;
    private int shulkerBoxCount = 0;
    private int nonEmptyCount = 0;
    private boolean stoppedByCap = false;

    public ContainerScanProcessor(
            World world,
            BoundingBox area,
            int radius,
            boolean nearFloorOnly,
            int scannedChunks,
            int skippedUnloadedChunks) {
        this.world = world;
        this.area = area;
        this.radius = radius;
        this.nearFloorOnly = nearFloorOnly;
        this.scannedChunks = scannedChunks;
        this.skippedUnloadedChunks = skippedUnloadedChunks;
    }

    /**
     * Scans one loaded chunk for supported container block states.
     *
     * @param chunk the loaded chunk to scan
     */
    public void scanChunk(Chunk chunk) {
        if (stoppedByCap) {
            return;
        }

        for (BlockState state : chunk.getTileEntities(false)) {
            if (stoppedByCap) {
                return;
            }

            Location loc = state.getLocation();
            if (!isInArea(loc)) {
                continue;
            }

            if (state instanceof Barrel barrel) {
                processBarrel(barrel);
            } else if (state instanceof Chest chest) {
                processChest(chest);
            } else if (state instanceof ShulkerBox shulkerBox) {
                processShulkerBox(shulkerBox);
            }
        }
    }

    public boolean isStoppedByCap() {
        return stoppedByCap;
    }

    public int getTotalContainers() {
        return totalContainers;
    }

    public ContainerScanCache buildCache(File textFile, File jsonFile) {
        String timestamp = LocalDateTime.now().format(FILE_TS);
        List<String> pageLines = buildPageLines();
        int totalPages =
                pageLines.isEmpty()
                        ? 0
                        : (int) Math.ceil(pageLines.size() / (double) ContainerFinderManager.PAGE_SIZE);

        return new ContainerScanCache(
                timestamp,
                world.getName(),
                radius,
                nearFloorOnly,
                totalContainers,
                chestCount,
                trappedChestCount,
                doubleChestCount,
                barrelCount,
                shulkerBoxCount,
                nonEmptyCount,
                distinctItemGroups.size(),
                topEntries(directTotals, directLabels, 10),
                topEntries(nestedTotals, nestedLabels, 10),
                scannedChunks,
                skippedUnloadedChunks,
                stoppedByCap,
                nonEmptyRecords,
                pageLines,
                totalPages,
                textFile,
                jsonFile);
    }

    private void processBarrel(Barrel barrel) {
        if (hitLimit()) {
            return;
        }

        totalContainers++;
        barrelCount++;

        processContainer("Barrel", barrel.getLocation(), barrel.getInventory());
    }

    private void processShulkerBox(ShulkerBox shulkerBox) {
        if (hitLimit()) {
            return;
        }

        totalContainers++;
        shulkerBoxCount++;

        String label = "Shulker Box";
        if (shulkerBox.getColor() != null) {
            label = prettifyEnum(shulkerBox.getColor().name()) + " Shulker Box";
        }

        processContainer(label, shulkerBox.getLocation(), shulkerBox.getInventory());
    }

    private void processChest(Chest chest) {
        Inventory inventory = chest.getInventory();
        InventoryHolder holder = inventory.getHolder(false);

        if (holder instanceof DoubleChest doubleChest) {
            String key = buildDoubleChestKey(doubleChest);
            if (!seenDoubleChests.add(key)) {
                return;
            }

            if (hitLimit()) {
                return;
            }

            totalContainers++;
            doubleChestCount++;

            Location loc = chooseLowerDoubleChestLocation(doubleChest);
            processContainer("Double Chest", loc, inventory);
            return;
        }

        if (hitLimit()) {
            return;
        }

        Material type = chest.getBlock().getType();
        String label = type == Material.TRAPPED_CHEST ? "Trapped Chest" : "Chest";

        totalContainers++;
        if (type == Material.TRAPPED_CHEST) {
            trappedChestCount++;
        } else {
            chestCount++;
        }

        processContainer(label, chest.getLocation(), inventory);
    }

    private boolean hitLimit() {
        if (totalContainers >= ContainerFinderManager.CONTAINER_LIMIT) {
            stoppedByCap = true;
            return true;
        }
        return false;
    }

    private void processContainer(String containerType, Location location, Inventory inventory) {
        Map<String, Long> directCounts = new LinkedHashMap<>();
        Map<String, String> directGroupLabels = new LinkedHashMap<>();
        Map<String, Long> nestedCounts = new LinkedHashMap<>();
        Map<String, String> nestedGroupLabels = new LinkedHashMap<>();

        boolean nonEmpty = false;
        ItemStack[] contents = inventory.getContents();

        for (ItemStack item : contents) {
            if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) {
                continue;
            }

            nonEmpty = true;

            ItemGroup directGroup = toItemGroup(item);
            mergeCount(directCounts, directGroupLabels, directGroup);
            mergeCount(directTotals, directLabels, directGroup);
            distinctItemGroups.add("DIRECT:" + directGroup.key);

            extractNestedShulkerContents(item, nestedCounts, nestedGroupLabels);
        }

        if (!nonEmpty) {
            return;
        }

        nonEmptyCount++;

        for (Map.Entry<String, Long> entry : nestedCounts.entrySet()) {
            String key = entry.getKey();
            String label = nestedGroupLabels.getOrDefault(key, key);
            long amount = entry.getValue();

            nestedTotals.merge(key, amount, Long::sum);
            nestedLabels.putIfAbsent(key, label);
            distinctItemGroups.add("NESTED:" + key);
        }

        nonEmptyRecords.add(
                new ContainerRecord(
                        containerType,
                        world.getName(),
                        location.getBlockX(),
                        location.getBlockY(),
                        location.getBlockZ(),
                        directCounts,
                        directGroupLabels,
                        nestedCounts,
                        nestedGroupLabels));
    }

    private void extractNestedShulkerContents(
            ItemStack item,
            Map<String, Long> nestedCounts,
            Map<String, String> nestedGroupLabels) {
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof BlockStateMeta blockStateMeta)) {
            return;
        }

        if (!blockStateMeta.hasBlockState()) {
            return;
        }

        BlockState state = blockStateMeta.getBlockState();
        if (!(state instanceof ShulkerBox shulkerBox)) {
            return;
        }

        for (ItemStack nested : shulkerBox.getInventory().getContents()) {
            if (nested == null || nested.getType() == Material.AIR || nested.getAmount() <= 0) {
                continue;
            }

            ItemGroup nestedGroup = toItemGroup(nested);
            mergeCount(nestedCounts, nestedGroupLabels, nestedGroup);
        }
    }

    private ItemGroup toItemGroup(ItemStack item) {
        Material material = item.getType();
        ItemMeta meta = item.getItemMeta();

        List<String> parts = new ArrayList<>();
        String baseLabel = prettifyEnum(material.name());

        if (meta != null) {
            String customName = extractDisplayName(meta);
            if (customName != null && !customName.isBlank()) {
                parts.add("Name=" + customName);
            }

            if (meta.hasEnchants()) {
                List<String> enchants = new ArrayList<>();
                meta.getEnchants()
                        .forEach((enchant, level) -> enchants.add(enchant.getKey().getKey() + ":" + level));
                Collections.sort(enchants);
                parts.add("Enchants=" + String.join(",", enchants));
            }

            if (meta instanceof PotionMeta potionMeta) {
                List<String> potionParts = new ArrayList<>();

                if (potionMeta.hasBasePotionType() && potionMeta.getBasePotionType() != null) {
                    potionParts.add("Base=" + potionMeta.getBasePotionType().name());
                }

                if (potionMeta.hasCustomEffects()) {
                    List<String> effects = new ArrayList<>();
                    potionMeta.getCustomEffects()
                            .forEach(
                                    effect ->
                                            effects.add(
                                                    effect.getType().getKey().getKey()
                                                            + ":"
                                                            + effect.getAmplifier()
                                                            + ":"
                                                            + effect.getDuration()));
                    Collections.sort(effects);
                    potionParts.add("Effects=" + String.join(",", effects));
                }

                if (!potionParts.isEmpty()) {
                    parts.add("Potion=" + String.join("|", potionParts));
                }
            }

            if (meta instanceof BookMeta bookMeta) {
                List<String> bookParts = new ArrayList<>();

                if (bookMeta.hasTitle() && bookMeta.getTitle() != null) {
                    bookParts.add("Title=" + bookMeta.getTitle());
                }
                if (bookMeta.hasAuthor() && bookMeta.getAuthor() != null) {
                    bookParts.add("Author=" + bookMeta.getAuthor());
                }

                if (!bookParts.isEmpty()) {
                    parts.add("Book=" + String.join("|", bookParts));
                }
            }

            List<Component> lore = meta.lore();
            if (meta.hasLore() && lore != null && !lore.isEmpty()) {
                List<String> loreLines = new ArrayList<>();
                for (Component line : lore) {
                    loreLines.add(PlainTextComponentSerializer.plainText().serialize(line));
                }
                parts.add("Lore=" + String.join(" / ", loreLines));
            }

            if (meta instanceof BlockStateMeta blockStateMeta && blockStateMeta.hasBlockState()) {
                BlockState blockState = blockStateMeta.getBlockState();
                if (blockState instanceof ShulkerBox shulkerBox && shulkerBox.getColor() != null) {
                    parts.add("ShulkerColor=" + shulkerBox.getColor().name());
                }
            }
        }

        String key = material.name();
        String label = baseLabel;

        if (!parts.isEmpty()) {
            key += "|" + String.join("|", parts);
            label += " {" + String.join("; ", parts) + "}";
        }

        return new ItemGroup(key, label, item.getAmount());
    }

    private String extractDisplayName(ItemMeta meta) {
        try {
            Component component = meta.displayName();
            if (component != null) {
                return PlainTextComponentSerializer.plainText().serialize(component);
            }
        } catch (Throwable ignored) {
            // safe fallback
        }

        return null;
    }

    private void mergeCount(
            Map<String, Long> counts, Map<String, String> labels, ItemGroup group) {
        counts.merge(group.key, (long) group.amount, Long::sum);
        labels.putIfAbsent(group.key, group.label);
    }

    private boolean isInArea(Location location) {
        return area.contains(location.getX(), location.getY(), location.getZ());
    }

    private String buildDoubleChestKey(DoubleChest doubleChest) {
        List<Location> locations = extractDoubleChestLocations(doubleChest);
        if (locations.size() == 2) {
            String a = locationKey(locations.get(0));
            String b = locationKey(locations.get(1));
            return a.compareTo(b) <= 0 ? a + "|" + b : b + "|" + a;
        }

        return locationKey(doubleChest.getLocation());
    }

    private Location chooseLowerDoubleChestLocation(DoubleChest doubleChest) {
        List<Location> locations = extractDoubleChestLocations(doubleChest);
        if (locations.isEmpty()) {
            return doubleChest.getLocation();
        }

        locations.sort(this::compareLocation);
        return locations.get(0);
    }

    private List<Location> extractDoubleChestLocations(DoubleChest doubleChest) {
        List<Location> locations = new ArrayList<>(2);

        addHolderLocation(locations, doubleChest.getLeftSide());
        addHolderLocation(locations, doubleChest.getRightSide());

        return locations;
    }

    private void addHolderLocation(List<Location> locations, InventoryHolder holder) {
        if (holder instanceof Chest chest) {
            locations.add(chest.getLocation());
        }
    }

    private int compareLocation(Location a, Location b) {
        int cmpX = Integer.compare(a.getBlockX(), b.getBlockX());
        if (cmpX != 0) return cmpX;

        int cmpY = Integer.compare(a.getBlockY(), b.getBlockY());
        if (cmpY != 0) return cmpY;

        return Integer.compare(a.getBlockZ(), b.getBlockZ());
    }

    private String locationKey(Location loc) {
        return String.format(
                "%s:%d:%d:%d",
                loc.getWorld() == null ? "unknown" : loc.getWorld().getName(),
                loc.getBlockX(),
                loc.getBlockY(),
                loc.getBlockZ());
    }

    private String prettifyEnum(String raw) {
        String[] parts = raw.toLowerCase(Locale.ROOT).split("_");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return out.toString();
    }

    private List<String> buildPageLines() {
        List<String> lines = new ArrayList<>();

        for (ContainerRecord record : nonEmptyRecords) {
            lines.add(record.toChatSummaryLine());

            if (!record.getDirectCounts().isEmpty()) {
                lines.add("§7  Direct:");
                for (String line : formatGroupLines(record.getDirectCounts(), record.getDirectLabels())) {
                    lines.add("§7    - §f" + line);
                }
            }

            if (!record.getNestedCounts().isEmpty()) {
                lines.add("§7  Nested shulker contents:");
                for (String line : formatGroupLines(record.getNestedCounts(), record.getNestedLabels())) {
                    lines.add("§7    - §d" + line);
                }
            }
        }

        return lines;
    }

    private List<String> topEntries(
            Map<String, Long> counts, Map<String, String> labels, int limit) {
        List<Map.Entry<String, Long>> sorted = new ArrayList<>(counts.entrySet());
        sorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

        List<String> out = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, sorted.size()); i++) {
            Map.Entry<String, Long> entry = sorted.get(i);
            String label = labels.getOrDefault(entry.getKey(), entry.getKey());
            out.add(label + " x" + entry.getValue());
        }
        return out;
    }

    private List<String> formatGroupLines(
            Map<String, Long> counts, Map<String, String> labels) {
        List<Map.Entry<String, Long>> entries = new ArrayList<>(counts.entrySet());
        entries.sort((a, b) -> {
            int cmp = Long.compare(b.getValue(), a.getValue());
            if (cmp != 0) return cmp;
            return labels.getOrDefault(a.getKey(), a.getKey())
                    .compareToIgnoreCase(labels.getOrDefault(b.getKey(), b.getKey()));
        });

        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, Long> entry : entries) {
            lines.add(labels.getOrDefault(entry.getKey(), entry.getKey()) + " x" + entry.getValue());
        }
        return lines;
    }

    private static final class ItemGroup {
        private final String key;
        private final String label;
        private final int amount;

        private ItemGroup(String key, String label, int amount) {
            this.key = key;
            this.label = label;
            this.amount = amount;
        }
    }
}