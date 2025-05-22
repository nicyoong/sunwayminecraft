package com.sunwayMinecraft.benches;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Represents a 3D cuboid region in the Minecraft world, defined by two corner locations. The region
 * is defined by a minimum and maximum coordinate in the X, Y, and Z dimensions.
 *
 * <p>The CuboidRegion class allows for easy access to the boundaries of the region, checks to see
 * if a specific location is within the region, and retrieves the world associated with the region.
 * The region is initialized using two `Location` objects that define opposite corners of the
 * cuboid.
 *
 * <p>Key functionality includes: - The constructor initializes the region based on two given
 * locations (opposite corners). - Methods to retrieve the minimum and maximum corners of the
 * region. - A method `contains()` to check if a given location is within the boundaries of the
 * cuboid. - Access to the world the region belongs to.
 *
 * <p>The main methods provided by this class are: - `getWorldName()`: Returns the name of the world
 * the region is located in. - `getMin()`: Returns the minimum corner of the region as a `Location`.
 * - `getMax()`: Returns the maximum corner of the region as a `Location`. - `getWorld()`: Returns
 * the world instance associated with the region. - `contains()`: Checks if a given location is
 * inside the cuboid region.
 *
 * <p>The region is defined in terms of the `minX`, `minY`, `minZ`, `maxX`, `maxY`, and `maxZ`
 * coordinates, which are determined by the two provided locations.
 */
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

  public Location getMax() {
    return new Location(getWorld(), maxX, maxY, maxZ);
  }

  private World getWorld() {
    return Bukkit.getWorld(worldName);
  }

  public boolean contains(Location location) {
    return location.getWorld().getName().equals(worldName)
        && location.getBlockX() >= minX
        && location.getBlockX() <= maxX
        && location.getBlockY() >= minY
        && location.getBlockY() <= maxY
        && location.getBlockZ() >= minZ
        && location.getBlockZ() <= maxZ;
  }
}
