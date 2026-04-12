package com.sunwayMinecraft.containerfinder;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Schedules and coordinates a batched container scan over loaded chunks.
 *
 * <p>This class intentionally focuses on:
 *
 * <ul>
 *   <li>Tick-based batching
 *   <li>Progress messages
 *   <li>Report writing
 *   <li>Passing final cache data back to the manager
 * </ul>
 *
 * <p>The actual scan and aggregation logic lives in {@link ContainerScanProcessor}.
 */
public class ContainerSearchTask extends BukkitRunnable {
    private static final int CHUNKS_PER_TICK = 4;
    private static final int PROGRESS_EVERY_N_CHUNKS = 10;

    private final JavaPlugin plugin;
    private final CommandSender sender;
    private final ContainerFinderManager manager;
    private final World world;
    private final BoundingBox area;
    private final int radius;
    private final boolean nearFloorOnly;
    private final List<Chunk> chunks;
    private final ContainerScanProcessor processor;

    private int chunkIndex = 0;

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
        this.processor =
                new ContainerScanProcessor(
                        world, area, radius, nearFloorOnly, chunks.size(), skippedUnloadedChunks);
    }

    @Override
    public void run() {
        int processedThisTick = 0;

        while (processedThisTick < CHUNKS_PER_TICK
                && chunkIndex < chunks.size()
                && !processor.isStoppedByCap()) {
            Chunk chunk = chunks.get(chunkIndex);
            processor.scanChunk(chunk);

            chunkIndex++;
            processedThisTick++;

            if (chunkIndex % PROGRESS_EVERY_N_CHUNKS == 0 || chunkIndex == chunks.size()) {
                sender.sendMessage(
                        String.format(
                                "§7Scanned chunks: §f%d/%d §7| Containers found: §f%d",
                                chunkIndex, chunks.size(), processor.getTotalContainers()));
            }
        }

        if (chunkIndex >= chunks.size() || processor.isStoppedByCap()) {
            finish();
            cancel();
        }
    }

    private void finish() {
        try {
            File reportsDir = new File(plugin.getDataFolder(), "reports");
            if (!reportsDir.exists() && !reportsDir.mkdirs()) {
                throw new IOException("Could not create reports directory: " + reportsDir.getAbsolutePath());
            }

            String baseName = "container-scan-" + java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

            File textFile = new File(reportsDir, baseName + ".txt");
            File jsonFile = new File(reportsDir, baseName + ".json");

            ContainerScanCache cache = processor.buildCache(textFile, jsonFile);

            ContainerReportWriter.writeTextReport(cache, textFile);
            ContainerReportWriter.writeJsonReport(cache, jsonFile);

            manager.setSearchComplete(cache);
            sendFinalSummary(cache);
        } catch (Exception e) {
            manager.markSearchCompleteWithoutCache();
            sender.sendMessage("§cContainer scan finished, but writing the report failed.");
            plugin.getLogger().severe("Container scan report write failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendFinalSummary(ContainerScanCache cache) {
        sender.sendMessage("§aContainer scan complete.");
        sender.sendMessage(
                String.format(
                        "§aFound §e%d §acontainers. §7(Chests: §e%d§7, Trapped: §e%d§7, Double: §e%d§7, Barrels: §e%d§7, Shulkers: §e%d§7)",
                        cache.getTotalContainers(),
                        cache.getChestCount(),
                        cache.getTrappedChestCount(),
                        cache.getDoubleChestCount(),
                        cache.getBarrelCount(),
                        cache.getShulkerBoxCount()));

        sender.sendMessage(
                String.format(
                        "§aNon-empty containers: §e%d §7| Distinct item groups: §e%d",
                        cache.getNonEmptyCount(),
                        cache.getDistinctItemGroups()));

        sender.sendMessage(
                String.format(
                        "§aChunks scanned: §e%d §7| Skipped unloaded: §e%d",
                        cache.getScannedChunks(),
                        cache.getSkippedUnloadedChunks()));

        if (cache.isStoppedByCap()) {
            sender.sendMessage(
                    String.format(
                            "§eScan stopped early after reaching the safety cap of %d containers.",
                            ContainerFinderManager.CONTAINER_LIMIT));
        }

        sendTopItems("Top direct items", cache.getTopDirectItems());
        sendTopItems("Top nested shulker items", cache.getTopNestedItems());

        if (cache.getTotalPages() > 0) {
            sender.sendMessage(
                    String.format(
                            "§aPages available: §e%d §7| Use §f/findcontainers page 1 §7to view details.",
                            cache.getTotalPages()));
        } else {
            sender.sendMessage("§eNo non-empty containers were found to page through.");
        }

        sender.sendMessage("§7Text report: §f" + cache.getTextReportFile().getAbsolutePath());
        sender.sendMessage("§7JSON report: §f" + cache.getJsonReportFile().getAbsolutePath());
    }

    private void sendTopItems(String title, List<String> entries) {
        sender.sendMessage("§a" + title + ":");
        if (entries.isEmpty()) {
            sender.sendMessage("§7  None");
            return;
        }

        for (String entry : entries) {
            sender.sendMessage("§7  - §f" + entry);
        }
    }
}