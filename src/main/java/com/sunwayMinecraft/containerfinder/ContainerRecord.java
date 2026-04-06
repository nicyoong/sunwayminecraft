package com.sunwayMinecraft.containerfinder;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents one non-empty logical container result.
 */
public class ContainerRecord {
    private final String containerType;
    private final String worldName;
    private final int x;
    private final int y;
    private final int z;
    private final Map<String, Long> directCounts;
    private final Map<String, String> directLabels;
    private final Map<String, Long> nestedCounts;
    private final Map<String, String> nestedLabels;

    public ContainerRecord(
            String containerType,
            String worldName,
            int x,
            int y,
            int z,
            Map<String, Long> directCounts,
            Map<String, String> directLabels,
            Map<String, Long> nestedCounts,
            Map<String, String> nestedLabels) {
        this.containerType = containerType;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.directCounts = new LinkedHashMap<>(directCounts);
        this.directLabels = new LinkedHashMap<>(directLabels);
        this.nestedCounts = new LinkedHashMap<>(nestedCounts);
        this.nestedLabels = new LinkedHashMap<>(nestedLabels);
    }

    public String getContainerType() {
        return containerType;
    }

    public String getWorldName() {
        return worldName;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public Map<String, Long> getDirectCounts() {
        return Collections.unmodifiableMap(directCounts);
    }

    public Map<String, String> getDirectLabels() {
        return Collections.unmodifiableMap(directLabels);
    }

    public Map<String, Long> getNestedCounts() {
        return Collections.unmodifiableMap(nestedCounts);
    }

    public Map<String, String> getNestedLabels() {
        return Collections.unmodifiableMap(nestedLabels);
    }

    public String toChatSummaryLine() {
        return String.format(
                "§e%s §7at §b%s §7X:§f%d §7Y:§f%d §7Z:§f%d",
                containerType, worldName, x, y, z);
    }
}