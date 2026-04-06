package com.sunwayMinecraft.containerfinder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Writes scan reports to text and JSON.
 */
public final class ContainerReportWriter {
    private ContainerReportWriter() {}

    public static void writeTextReport(ContainerScanCache cache, File file) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("Container Scan Report");
            writer.newLine();
            writer.write("====================");
            writer.newLine();
            writer.write("Timestamp: " + cache.getTimestamp());
            writer.newLine();
            writer.write("World: " + cache.getWorldName());
            writer.newLine();
            writer.write("Radius: " + cache.getRadius());
            writer.newLine();
            writer.write("Near floor only: " + cache.isNearFloorOnly());
            writer.newLine();
            writer.write("Scanned chunks: " + cache.getScannedChunks());
            writer.newLine();
            writer.write("Skipped unloaded chunks: " + cache.getSkippedUnloadedChunks());
            writer.newLine();
            writer.write("Stopped by cap: " + cache.isStoppedByCap());
            writer.newLine();
            writer.newLine();

            writer.write("Summary");
            writer.newLine();
            writer.write("-------");
            writer.newLine();
            writer.write("Total containers: " + cache.getTotalContainers());
            writer.newLine();
            writer.write("Chest: " + cache.getChestCount());
            writer.newLine();
            writer.write("Trapped Chest: " + cache.getTrappedChestCount());
            writer.newLine();
            writer.write("Double Chest: " + cache.getDoubleChestCount());
            writer.newLine();
            writer.write("Barrel: " + cache.getBarrelCount());
            writer.newLine();
            writer.write("Non-empty containers: " + cache.getNonEmptyCount());
            writer.newLine();
            writer.write("Distinct item groups: " + cache.getDistinctItemGroups());
            writer.newLine();
            writer.newLine();

            writer.write("Top direct items");
            writer.newLine();
            writer.write("----------------");
            writer.newLine();
            if (cache.getTopDirectItems().isEmpty()) {
                writer.write("None");
                writer.newLine();
            } else {
                for (String line : cache.getTopDirectItems()) {
                    writer.write("- " + line);
                    writer.newLine();
                }
            }
            writer.newLine();

            writer.write("Top nested shulker items");
            writer.newLine();
            writer.write("------------------------");
            writer.newLine();
            if (cache.getTopNestedItems().isEmpty()) {
                writer.write("None");
                writer.newLine();
            } else {
                for (String line : cache.getTopNestedItems()) {
                    writer.write("- " + line);
                    writer.newLine();
                }
            }
            writer.newLine();

            writer.write("Non-empty container details");
            writer.newLine();
            writer.write("---------------------------");
            writer.newLine();

            for (ContainerRecord record : cache.getRecords()) {
                writer.write(
                        String.format(
                                "%s @ %s X:%d Y:%d Z:%d",
                                record.getContainerType(),
                                record.getWorldName(),
                                record.getX(),
                                record.getY(),
                                record.getZ()));
                writer.newLine();

                if (!record.getDirectCounts().isEmpty()) {
                    writer.write("  Direct:");
                    writer.newLine();
                    for (String line : sortedLines(record.getDirectCounts(), record.getDirectLabels())) {
                        writer.write("    - " + line);
                        writer.newLine();
                    }
                }

                if (!record.getNestedCounts().isEmpty()) {
                    writer.write("  Nested shulker contents:");
                    writer.newLine();
                    for (String line : sortedLines(record.getNestedCounts(), record.getNestedLabels())) {
                        writer.write("    - " + line);
                        writer.newLine();
                    }
                }

                writer.newLine();
            }
        }
    }

    public static void writeJsonReport(ContainerScanCache cache, File file) throws IOException {
        Map<String, Object> root = new LinkedHashMap<>();

        root.put("timestamp", cache.getTimestamp());
        root.put("world", cache.getWorldName());
        root.put("radius", cache.getRadius());
        root.put("nearFloorOnly", cache.isNearFloorOnly());
        root.put("scannedChunks", cache.getScannedChunks());
        root.put("skippedUnloadedChunks", cache.getSkippedUnloadedChunks());
        root.put("stoppedByCap", cache.isStoppedByCap());

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalContainers", cache.getTotalContainers());
        summary.put("chestCount", cache.getChestCount());
        summary.put("trappedChestCount", cache.getTrappedChestCount());
        summary.put("doubleChestCount", cache.getDoubleChestCount());
        summary.put("barrelCount", cache.getBarrelCount());
        summary.put("nonEmptyCount", cache.getNonEmptyCount());
        summary.put("distinctItemGroups", cache.getDistinctItemGroups());
        summary.put("topDirectItems", cache.getTopDirectItems());
        summary.put("topNestedItems", cache.getTopNestedItems());
        root.put("summary", summary);

        List<Object> containers = new ArrayList<>();
        for (ContainerRecord record : cache.getRecords()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("type", record.getContainerType());
            entry.put("world", record.getWorldName());
            entry.put("x", record.getX());
            entry.put("y", record.getY());
            entry.put("z", record.getZ());
            entry.put("directItems", groupsAsJson(record.getDirectCounts(), record.getDirectLabels()));
            entry.put("nestedShulkerItems", groupsAsJson(record.getNestedCounts(), record.getNestedLabels()));
            containers.add(entry);
        }
        root.put("containers", containers);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(JsonUtil.toJson(root));
        }
    }

    private static List<Object> groupsAsJson(
            Map<String, Long> counts, Map<String, String> labels) {
        List<Object> list = new ArrayList<>();

        List<Map.Entry<String, Long>> entries = new ArrayList<>(counts.entrySet());
        entries.sort((a, b) -> {
            int cmp = Long.compare(b.getValue(), a.getValue());
            if (cmp != 0) return cmp;
            return labels.getOrDefault(a.getKey(), a.getKey())
                    .compareToIgnoreCase(labels.getOrDefault(b.getKey(), b.getKey()));
        });

        for (Map.Entry<String, Long> entry : entries) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("key", entry.getKey());
            row.put("label", labels.getOrDefault(entry.getKey(), entry.getKey()));
            row.put("amount", entry.getValue());
            list.add(row);
        }

        return list;
    }

    private static List<String> sortedLines(
            Map<String, Long> counts, Map<String, String> labels) {
        List<Map.Entry<String, Long>> entries = new ArrayList<>(counts.entrySet());
        entries.sort((a, b) -> {
            int cmp = Long.compare(b.getValue(), a.getValue());
            if (cmp != 0) return cmp;
            return labels.getOrDefault(a.getKey(), a.getKey())
                    .compareToIgnoreCase(labels.getOrDefault(b.getKey(), b.getKey()));
        });

        List<String> out = new ArrayList<>();
        for (Map.Entry<String, Long> entry : entries) {
            out.add(labels.getOrDefault(entry.getKey(), entry.getKey()) + " x" + entry.getValue());
        }
        return out;
    }
}