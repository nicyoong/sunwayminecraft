package com.sunwayMinecraft.districts.region;

import org.bukkit.Location;

import java.util.Locale;
import java.util.Objects;

/**
 * A district's physical shape: either a cuboid box (the original region
 * format) or a point with a radius (spherical). Location containment checks
 * are world-aware.
 */
public final class DistrictShape {
    public enum Kind { CUBOID, POINT_RADIUS }

    private final Kind kind;
    private final Region3i cuboid;
    private final String world;
    private final double centerX;
    private final double centerY;
    private final double centerZ;
    private final double radius;

    private DistrictShape(Kind kind, Region3i cuboid, String world,
                          double centerX, double centerY, double centerZ, double radius) {
        this.kind = kind;
        this.cuboid = cuboid;
        this.world = world;
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.radius = radius;
    }

    public static DistrictShape cuboid(Region3i region) {
        Objects.requireNonNull(region, "region");
        return new DistrictShape(Kind.CUBOID, region, region.getWorld(), 0, 0, 0, 0);
    }

    public static DistrictShape pointRadius(String world, double cx, double cy, double cz, double radius) {
        if (world == null || world.isBlank()) {
            throw new IllegalArgumentException("point_radius requires a world");
        }
        if (radius <= 0) {
            throw new IllegalArgumentException("point_radius requires a positive radius");
        }
        return new DistrictShape(Kind.POINT_RADIUS, null, world, cx, cy, cz, radius);
    }

    public Kind getKind() {
        return kind;
    }

    public String getWorld() {
        return world;
    }

    public double getCenterX() {
        return centerX;
    }

    public double getCenterY() {
        return centerY;
    }

    public double getCenterZ() {
        return centerZ;
    }

    public double getRadius() {
        return radius;
    }

    /** The cuboid region, or null for point-radius shapes. */
    public Region3i getCuboid() {
        return cuboid;
    }

    public boolean contains(Location location) {
        if (location == null) return false;
        if (kind == Kind.CUBOID) {
            return cuboid.contains(location);
        }
        if (!location.getWorld().getName().equals(world)) {
            return false;
        }
        double dx = location.getX() - centerX;
        double dy = location.getY() - centerY;
        double dz = location.getZ() - centerZ;
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }

    /** Approximate block volume, used to prefer the more specific district. */
    public long getVolume() {
        if (kind == Kind.CUBOID) {
            return cuboid.getVolume();
        }
        return (long) (4.0 / 3.0 * Math.PI * radius * radius * radius);
    }

    /**
     * Overlap check for validation; only two cuboids are compared -
     * spherical overlaps are not validated.
     */
    public boolean overlapsVolume(DistrictShape other) {
        if (kind != Kind.CUBOID || other == null || other.kind != Kind.CUBOID) {
            return false;
        }
        return cuboid.overlapsVolume(other.cuboid);
    }

    public String describe() {
        return kind == Kind.CUBOID
                ? "cuboid " + cuboid.getMinX() + ".." + cuboid.getMaxX() + " "
                    + cuboid.getMinY() + ".." + cuboid.getMaxY() + " "
                    + cuboid.getMinZ() + ".." + cuboid.getMaxZ() + " in " + cuboid.getWorld()
                : String.format(Locale.ROOT, "point_radius r=%.1f at (%.1f, %.1f, %.1f) in %s",
                    radius, centerX, centerY, centerZ, world);
    }
}
