package com.sunwayMinecraft.containerfinder;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.List;

public class ContainerFinderManager {
    public static final int DEFAULT_RADIUS = 96;
    public static final int PAGE_SIZE = 10;
    public static final int CONTAINER_LIMIT = 5000;

    private final JavaPlugin plugin;
    private boolean searchRunning = false;
    private ContainerScanCache lastScanCache;

    public ContainerFinderManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public int getDefaultRadius() {
        return DEFAULT_RADIUS;
    }

    public boolean isSearchRunning() {
        return searchRunning;
    }

    public void startSearch(Player player, int radius, boolean nearFloorOnly) {
        if (searchRunning) {
            player.sendMessage("§cA container scan is already in progress.");
            return;
        }

        World world = player.getWorld();
        BoundingBox area = createArea(player.getLocation(), radius, nearFloorOnly);

        int minChunkX = floorToChunk(area.getMinX());
        int maxChunkX = floorToChunk(area.getMaxX());
        int minChunkZ = floorToChunk(area.getMinZ());
        int maxChunkZ = floorToChunk(area.getMaxZ());

        List<Chunk> loadedChunks = new ArrayList<>();
        int skippedUnloaded = 0;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (world.isChunkLoaded(chunkX, chunkZ)) {
                    loadedChunks.add(world.getChunkAt(chunkX, chunkZ));
                } else {
                    skippedUnloaded++;
                }
            }
        }

        searchRunning = true;
        lastScanCache = null;

        player.sendMessage(
                String.format(
                        "§aStarting container scan in §e%s§a with radius §e%d§a. Loaded chunks: §e%d§a, skipped unloaded: §e%d§a.",
                        world.getName(), radius, loadedChunks.size(), skippedUnloaded));

        BukkitRunnable task =
                new ContainerSearchTask(
                        plugin,
                        player,
                        this,
                        world,
                        area,
                        radius,
                        nearFloorOnly,
                        loadedChunks,
                        skippedUnloaded);
        task.runTaskTimer(plugin, 0L, 1L);
    }

    public void setSearchComplete(ContainerScanCache cache) {
        this.lastScanCache = cache;
        this.searchRunning = false;
    }

    public void markSearchCompleteWithoutCache() {
        this.searchRunning = false;
    }

    public void sendPage(CommandSender sender, int page) {
        if (lastScanCache == null) {
            sender.sendMessage("§eNo completed container scan is available yet.");
            return;
        }

        int totalPages = lastScanCache.getTotalPages();
        if (totalPages == 0) {
            sender.sendMessage("§eThe last scan had no non-empty containers to page through.");
            return;
        }

        if (page > totalPages) {
            sender.sendMessage(
                    String.format("§cPage %d does not exist. Available pages: 1-%d.", page, totalPages));
            return;
        }

        sender.sendMessage(
                String.format(
                        "§aContainer scan results page §e%d/%d§a:", page, totalPages));

        for (String line : lastScanCache.getPageLines(page)) {
            sender.sendMessage(line);
        }

        sender.sendMessage(
                String.format(
                        "§7Use §f/findcontainers page <number> §7to view another page."));
    }

    private BoundingBox createArea(Location center, int radius, boolean nearFloorOnly) {
        World world = center.getWorld();
        if (world == null) {
            throw new IllegalStateException("Player world cannot be null.");
        }

        double minY;
        double maxY;

        if (nearFloorOnly) {
            minY = center.getBlockY() - 5;
            maxY = center.getBlockY() + 5;
        } else {
            minY = world.getMinHeight();
            maxY = world.getMaxHeight() - 1;
        }

        return new BoundingBox(
                center.getX() - radius,
                minY,
                center.getZ() - radius,
                center.getX() + radius,
                maxY,
                center.getZ() + radius);
    }

    private int floorToChunk(double value) {
        return ((int) Math.floor(value)) >> 4;
    }
}
