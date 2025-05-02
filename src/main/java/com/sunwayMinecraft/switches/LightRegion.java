package com.sunwayMinecraft.switches;

import org.bukkit.Location;
import org.bukkit.World;

public record LightRegion(
        String name,
        World world,
        int minX, int minY, int minZ,
        int maxX, int maxY, int maxZ
) {
    public boolean contains(Location loc) {
        return loc.getWorld().equals(world) &&
                loc.getBlockX() >= minX && loc.getBlockX() <= maxX &&
                loc.getBlockY() >= minY && loc.getBlockY() <= maxY &&
                loc.getBlockZ() >= minZ && loc.getBlockZ() <= maxZ;
    }
}
