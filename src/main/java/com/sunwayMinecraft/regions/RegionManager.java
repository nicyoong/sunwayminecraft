package com.sunwayMinecraft.regions;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

public class RegionManager {
    private final JavaPlugin plugin;
    private final RegionDatabase database;
    private final RegionRepository repository;
    private final RegionImporter importer;
    private final GriefPrevention griefPrevention;

    public RegionManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.database = new RegionDatabase(plugin);
        this.repository = new RegionRepository();
        this.importer = new RegionImporter();
        this.griefPrevention = initGriefPrevention();
    }

    private GriefPrevention initGriefPrevention() {
        if (plugin.getServer().getPluginManager().getPlugin("GriefPrevention") != null) {
            return GriefPrevention.instance;
        }
        return null;
    }

    public void initialize() {
        try {
            database.connect();
            database.initializeSchema();
            loadRegions();
            importer.importLegacyRegions(plugin, this);

            // Add GP file watcher if GP is installed
            if (griefPrevention != null) {
                GPClaimWatcher watcher = new GPClaimWatcher(plugin, this);
                watcher.run(); // Initial import
                // Run every 10 seconds (200 ticks)
                plugin.getServer().getScheduler().runTaskTimer(plugin, watcher, 200, 200);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Database initialization failed", e);
        }
    }

    private void loadRegions() throws SQLException {
        for (Region region : database.loadAllRegions()) {
            repository.addRegion(region);
        }
    }

    public boolean createRegion(String name, String world, int minX, int minY, int minZ,
                                int maxX, int maxY, int maxZ, Long claimId, boolean decoupled) {
        if (minX > maxX || minY > maxY || minZ > maxZ) return false;
        if (repository.getByName(name) != null) return false;

        try {
            int id = database.createRegion(name, world, minX, minY, minZ,
                    maxX, maxY, maxZ, claimId, decoupled);
            if (id != -1) {
                Region region = new Region(id, name, world, minX, minY, minZ,
                        maxX, maxY, maxZ, claimId, decoupled, new HashSet<>());
                repository.addRegion(region);
                return true;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create region: " + name, e);
        }
        return false;
    }

    public boolean updateRegionBounds(String name, int minX, int minY, int minZ,
                                      int maxX, int maxY, int maxZ) {
        Region region = repository.getByName(name);
        if (region == null || region.isDecoupled()) return false;

        try {
            database.updateRegionBounds(region.getId(), minX, minY, minZ, maxX, maxY, maxZ);
            region.updateBounds(minX, minY, minZ, maxX, maxY, maxZ);
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to update region: " + name, e);
        }
        return false;
    }

    public boolean deleteRegion(String name) {
        Region region = repository.getByName(name);
        if (region == null) return false;

        try {
            database.deleteRegion(region.getId());
            repository.removeRegion(region);
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete region: " + name, e);
        }
        return false;
    }

    public boolean setDecoupled(String name, boolean decoupled) {
        Region region = repository.getByName(name);
        if (region == null) return false;

        try {
            database.setDecoupled(region.getId(), decoupled);
            region.setDecoupled(decoupled);
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to update region: " + name, e);
        }
        return false;
    }

    public boolean manageTrust(String name, UUID player, boolean add) {
        Region region = repository.getByName(name);
        if (region == null || !region.isDecoupled()) return false;

        try {
            database.manageTrust(region.getId(), player, add);
            if (add) {
                region.getTrustedPlayers().add(player);
            } else {
                region.getTrustedPlayers().remove(player);
            }
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to " + (add ? "add" : "remove") +
                    " trust for region: " + name, e);
        }
        return false;
    }

    // Delegate methods to repository
    public Map<String, Region> getRegions() {
        return repository.getAllRegions();
    }

    public List<Region> getRegionsAt(Location location) {
        return repository.getRegionsAt(location);
    }

    public Region getRegionByName(String name) {
        return repository.getByName(name);
    }

    public Region getRegionByClaimId(long claimId) {
        return repository.getByClaimId(claimId);
    }

    public void close() {
        database.close();
    }

    public boolean canModifyAtLocation(Player player, Location location) {
        List<Region> regions = getRegionsAt(location);
        if (regions.isEmpty()) return false;

        for (Region region : regions) {
            if (region.isDecoupled()) {
                // Handle decoupled region trust
                if (region.getTrustedPlayers().contains(player.getUniqueId())) {
                    return true;
                }
            } else {
                // Handle GP-coupled region
                if (hasGPAccess(player, region)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasAccessToRegion(Player player, Region region) {
        // Decoupled regions use trust lists
        if (region.isDecoupled()) {
            return region.getTrustedPlayers().contains(player.getUniqueId());
        }
        // GP-linked regions use GP permissions
        else if (griefPrevention != null) {
            return hasGPAccess(player, region);
        }
        return false;
    }

    private boolean hasGPAccess(Player player, Region region) {
        if (region.getClaimId() == null) return false;
        Claim claim = griefPrevention.dataStore.getClaim(region.getClaimId());
        if (claim == null) return false;

        // First check access trust (minimum required for switches)
        if (claim.checkPermission(player, ClaimPermission.Access, null) == null) {
            return true;
        }

        // Also allow if player has higher trust levels
        return claim.checkPermission(player, ClaimPermission.Build, null) == null ||
                claim.checkPermission(player, ClaimPermission.Inventory, null) == null ||
                claim.checkPermission(player, ClaimPermission.Edit, null) == null;
    }
}