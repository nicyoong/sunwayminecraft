package com.sunwayMinecraft.regions;

import org.bukkit.Location;
import org.bukkit.World;
import java.util.Set;
import java.util.UUID;

public class Region {
    private final int id;
    private final String name;
    private final String worldName;
    private int minX, minY, minZ;
    private int maxX, maxY, maxZ;
    private Long claimId;
    private boolean decoupled;
    private final Set<UUID> trustedPlayers;

    public Region(int id, String name, String worldName,
                  int minX, int minY, int minZ,
                  int maxX, int maxY, int maxZ,
                  Long claimId, boolean decoupled,
                  Set<UUID> trustedPlayers) {
        this.id = id;
        this.name = name;
        this.worldName = worldName;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.claimId = claimId;
        this.decoupled = decoupled;
        this.trustedPlayers = trustedPlayers;
    }

    public boolean contains(Location loc) {
        World world = loc.getWorld();
        if (world == null || !world.getName().equals(worldName)) return false;

        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        return x >= minX && x <= maxX &&
                y >= minY && y <= maxY &&
                z >= minZ && z <= maxZ;
    }

    public void updateBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    // Getters and setters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getWorldName() { return worldName; }
    public int getMinX() { return minX; }
    public int getMinY() { return minY; }
    public int getMinZ() { return minZ; }
    public int getMaxX() { return maxX; }
    public int getMaxY() { return maxY; }
    public int getMaxZ() { return maxZ; }
    public Long getClaimId() { return claimId; }
    public boolean isDecoupled() { return decoupled; }
    public Set<UUID> getTrustedPlayers() { return trustedPlayers; }
    public void setDecoupled(boolean decoupled) { this.decoupled = decoupled; }
}