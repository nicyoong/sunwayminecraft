package com.sunwayMinecraft.benches;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class CuboidRegion {
    private final String worldName;
    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;

    public CuboidRegion(String worldName, Location pos1, Location pos2) {
        this.worldName = worldName;

        this.minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        this.minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        this.minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());

        this.maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        this.maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        this.maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
    }

    // New methods for data access
    public String getWorldName() {
        return worldName;
    }

    public Location getMin() {
        return new Location(getWorld(), minX, minY, minZ);
    }

    public boolean contains(Location location) {
        return location.getWorld().getName().equals(worldName) &&
                location.getBlockX() >= minX && location.getBlockX() <= maxX &&
                location.getBlockY() >= minY && location.getBlockY() <= maxY &&
                location.getBlockZ() >= minZ && location.getBlockZ() <= maxZ;
    }
}