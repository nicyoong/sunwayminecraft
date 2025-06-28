// GPClaimWatcher.java
package com.sunwayMinecraft.regions;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class GPClaimWatcher implements Runnable {
    private static final Pattern COORD_PATTERN = Pattern.compile(";");

    private final JavaPlugin plugin;
    private final RegionManager regionManager;
    private final File gpDataDir;
    private final Map<String, Long> lastModifiedCache = new HashMap<>();

    public GPClaimWatcher(JavaPlugin plugin, RegionManager regionManager) {
        this.plugin = plugin;
        this.regionManager = regionManager;
        this.gpDataDir = new File(plugin.getDataFolder().getParentFile(), "GriefPreventionData/ClaimData");
    }

    @Override
    public void run() {
        if (!gpDataDir.exists() || !gpDataDir.isDirectory()) return;

        File[] claimFiles = gpDataDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (claimFiles == null) return;

        Set<String> currentFiles = new HashSet<>();
        for (File file : claimFiles) {
            String fileName = file.getName();
            currentFiles.add(fileName);
            long lastModified = file.lastModified();

            // Process new/modified files
            if (!lastModifiedCache.containsKey(fileName) || lastModifiedCache.get(fileName) != lastModified) {
                processClaimFile(file);
                lastModifiedCache.put(fileName, lastModified);
            }
        }

        // Handle deleted files
        Iterator<String> cacheIterator = lastModifiedCache.keySet().iterator();
        while (cacheIterator.hasNext()) {
            String cachedFile = cacheIterator.next();
            if (!currentFiles.contains(cachedFile)) {
                String regionName = cachedFile.replace(".yml", "");
                regionManager.deleteRegion(regionName);
                cacheIterator.remove();
            }
        }
    }

    private void processClaimFile(File claimFile) {
        try {
            String regionName = claimFile.getName().replace(".yml", "");
            YamlConfiguration config = YamlConfiguration.loadConfiguration(claimFile);

            String[] minParts = COORD_PATTERN.split(config.getString("Lesser Boundary Corner", ""));
            String[] maxParts = COORD_PATTERN.split(config.getString("Greater Boundary Corner", ""));

            if (minParts.length < 4 || maxParts.length < 4) {
                plugin.getLogger().warning("Invalid claim format: " + claimFile.getName());
                return;
            }

            String world = minParts[0];
            int minX = Integer.parseInt(minParts[1]);
            int minY = Integer.parseInt(minParts[2]);
            int minZ = Integer.parseInt(minParts[3]);
            int maxX = Integer.parseInt(maxParts[1]);
            int maxY = Integer.parseInt(maxParts[2]);
            int maxZ = Integer.parseInt(maxParts[3]);
            long claimId = Long.parseLong(regionName);

            Region existing = regionManager.getRegionByName(regionName);
            if (existing == null) {
                // Create new region
                regionManager.createRegion(
                        regionName, world,
                        minX, minY, minZ,
                        maxX, maxY, maxZ,
                        claimId, false
                );
            } else if (!existing.isDecoupled()) {
                // Update existing non-decoupled region
                regionManager.updateRegionBounds(
                        regionName,
                        minX, minY, minZ,
                        maxX, maxY, maxZ
                );
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to process GP claim: " + claimFile.getName(), e);
        }
    }
}