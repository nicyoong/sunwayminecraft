package com.sunwayMinecraft.districts.region;

import org.bukkit.Location;

import java.util.Objects;

public class Region3i {
    private final String world;
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;

    public Region3i(String world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.world = Objects.requireNonNull(world, "world");
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);
    }

    public String getWorld() { return world; }
    public int getMinX() { return minX; }
    public int getMinY() { return minY; }
    public int getMinZ() { return minZ; }
    public int getMaxX() { return maxX; }
    public int getMaxY() { return maxY; }
    public int getMaxZ() { return maxZ; }

    public long getVolume() {
        return (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }

    public boolean contains(Location location) {
        if (location == null || location.getWorld() == null) return false;
        if (!world.equalsIgnoreCase(location.getWorld().getName())) return false;
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        return x >= minX && x <= maxX
            && y >= minY && y <= maxY
            && z >= minZ && z <= maxZ;
    }

    public boolean overlapsVolume(Region3i other) {
        if (other == null) return false;
        if (!world.equalsIgnoreCase(other.world)) return false;

        return minX <= other.maxX && other.minX <= maxX
            && minY <= other.maxY && other.minY <= maxY
            && minZ <= other.maxZ && other.minZ <= maxZ;
    }

    @Override
    public String toString() {
        return world + " [" + minX + "," + minY + "," + minZ + " -> " + maxX + "," + maxY + "," + maxZ + "]";
    }
}
