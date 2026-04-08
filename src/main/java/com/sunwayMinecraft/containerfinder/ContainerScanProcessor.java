package com.sunwayMinecraft.containerfinder;

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
import org.bukkit.util.BoundingBox;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Performs the actual scanning and aggregation for container searches.
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
    private final ContainerItemAnalyzer itemAnalyzer;

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
        this.itemAnalyzer = new ContainerItemAnalyzer();
    }

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

            ContainerItemAnalyzer.ItemGroup directGroup = itemAnalyzer.toItemGroup(item);
            itemAnalyzer.mergeCount(directCounts, directGroupLabels, directGroup);
            itemAnalyzer.mergeCount(directTotals, directLabels, directGroup);
            distinctItemGroups.add("DIRECT:" + directGroup.key());

            itemAnalyzer.extractNestedShulkerContents(item, nestedCounts, nestedGroupLabels);
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
}