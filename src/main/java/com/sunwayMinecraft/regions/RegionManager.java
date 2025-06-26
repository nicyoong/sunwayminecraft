package com.sunwayMinecraft.regions;

import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

public class RegionManager {
    private final JavaPlugin plugin;
    private final RegionDatabase database;
    private final RegionRepository repository;
    private final RegionImporter importer;

    public RegionManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.database = new RegionDatabase(plugin);
        this.repository = new RegionRepository();
        this.importer = new RegionImporter();
    }

    public void initialize() {
        try {
            database.connect();
            database.initializeSchema();
            loadRegions();
            importer.importLegacyRegions(plugin, this);
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
}