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
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Batched task that scans loaded chunks for chests, double chests, trapped chests, and barrels.
 */
public class ContainerSearchTask extends BukkitRunnable {
    private static final int CHUNKS_PER_TICK = 4;
    private static final int PROGRESS_EVERY_N_CHUNKS = 10;
    private static final DateTimeFormatter FILE_TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final JavaPlugin plugin;
    private final CommandSender sender;
    private final ContainerFinderManager manager;
    private final World world;
    private final BoundingBox area;
    private final int radius;
    private final boolean nearFloorOnly;
    private final List<Chunk> chunks;
    private final int skippedUnloadedChunks;

    private final Set<String> seenDoubleChests = new HashSet<>();
    private final List<ContainerRecord> nonEmptyRecords = new ArrayList<>();
    private final Map<String, Long> directTotals = new LinkedHashMap<>();
    private final Map<String, String> directLabels = new LinkedHashMap<>();
    private final Map<String, Long> nestedTotals = new LinkedHashMap<>();
    private final Map<String, String> nestedLabels = new LinkedHashMap<>();
    private final Set<String> distinctItemGroups = new HashSet<>();

    private int chunkIndex = 0;
    private int totalContainers = 0;
    private int chestCount = 0;
    private int trappedChestCount = 0;
    private int doubleChestCount = 0;
    private int barrelCount = 0;
    private int nonEmptyCount = 0;
    private boolean stoppedByCap = false;

    public ContainerSearchTask(
            JavaPlugin plugin,
            CommandSender sender,
            ContainerFinderManager manager,
            World world,
            BoundingBox area,
            int radius,
            boolean nearFloorOnly,
            List<Chunk> chunks,
            int skippedUnloadedChunks) {
        this.plugin = plugin;
        this.sender = sender;
        this.manager = manager;
        this.world = world;
        this.area = area;
        this.radius = radius;
        this.nearFloorOnly = nearFloorOnly;
        this.chunks = new ArrayList<>(chunks);
        this.skippedUnloadedChunks = skippedUnloadedChunks;
    }

    @Override
    public void run() {
        int processedThisTick = 0;

        while (processedThisTick < CHUNKS_PER_TICK && chunkIndex < chunks.size() && !stoppedByCap) {
            Chunk chunk = chunks.get(chunkIndex);
            scanChunk(chunk);
            chunkIndex++;
            processedThisTick++;

            if (chunkIndex % PROGRESS_EVERY_N_CHUNKS == 0 || chunkIndex == chunks.size()) {
                sender.sendMessage(
                        String.format(
                                "§7Scanned chunks: §f%d/%d §7| Containers found: §f%d",
                                chunkIndex, chunks.size(), totalContainers));
            }
        }

        if (chunkIndex >= chunks.size() || stoppedByCap) {
            finish();
            cancel();
        }
    }

    private void scanChunk(Chunk chunk) {
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
            }
        }
    }

    private void processBarrel(Barrel barrel) {
        if (hitLimit()) {
            return;
        }

        totalContainers++;
        barrelCount++;

        Location loc = barrel.getLocation();
        Inventory inventory = barrel.getInventory();
        processContainer("Barrel", loc, inventory);
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

        ItemStack[] contents = inventory.getContents();
        boolean nonEmpty = false;

        for (ItemStack item : contents) {
            if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) {
                continue;
            }

            nonEmpty = true;
            ItemGroup group = toItemGroup(item);
            mergeCount(directCounts, directGroupLabels, group);

            distinctItemGroups.add("DIRECT:" + group.key);
            mergeCount(directTotals, directLabels, group);

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

        ContainerRecord record =
                new ContainerRecord(
                        containerType,
                        world.getName(),
                        location.getBlockX(),
                        location.getBlockY(),
                        location.getBlockZ(),
                        directCounts,
                        directGroupLabels,
                        nestedCounts,
                        nestedGroupLabels);

        nonEmptyRecords.add(record);
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

            ItemGroup group = toItemGroup(nested);
            mergeCount(nestedCounts, nestedGroupLabels, group);
        }
    }
}
