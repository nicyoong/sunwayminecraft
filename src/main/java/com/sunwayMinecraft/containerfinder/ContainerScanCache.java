package com.sunwayMinecraft.containerfinder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stores the last completed scan so pages can be viewed later.
 */
public class ContainerScanCache {
    private final String timestamp;
    private final String worldName;
    private final int radius;
    private final boolean nearFloorOnly;
    private final int totalContainers;
    private final int chestCount;
    private final int trappedChestCount;
    private final int doubleChestCount;
    private final int barrelCount;
    private final int nonEmptyCount;
    private final int distinctItemGroups;
    private final List<String> topDirectItems;
    private final List<String> topNestedItems;
    private final int scannedChunks;
    private final int skippedUnloadedChunks;
    private final boolean stoppedByCap;
    private final List<ContainerRecord> records;
    private final List<String> pageLines;
    private final int totalPages;
    private final File textReportFile;
    private final File jsonReportFile;

    public ContainerScanCache(
            String timestamp,
            String worldName,
            int radius,
            boolean nearFloorOnly,
            int totalContainers,
            int chestCount,
            int trappedChestCount,
            int doubleChestCount,
            int barrelCount,
            int nonEmptyCount,
            int distinctItemGroups,
            List<String> topDirectItems,
            List<String> topNestedItems,
            int scannedChunks,
            int skippedUnloadedChunks,
            boolean stoppedByCap,
            List<ContainerRecord> records,
            List<String> pageLines,
            int totalPages,
            File textReportFile,
            File jsonReportFile) {
        this.timestamp = timestamp;
        this.worldName = worldName;
        this.radius = radius;
        this.nearFloorOnly = nearFloorOnly;
        this.totalContainers = totalContainers;
        this.chestCount = chestCount;
        this.trappedChestCount = trappedChestCount;
        this.doubleChestCount = doubleChestCount;
        this.barrelCount = barrelCount;
        this.nonEmptyCount = nonEmptyCount;
        this.distinctItemGroups = distinctItemGroups;
        this.topDirectItems = new ArrayList<>(topDirectItems);
        this.topNestedItems = new ArrayList<>(topNestedItems);
        this.scannedChunks = scannedChunks;
        this.skippedUnloadedChunks = skippedUnloadedChunks;
        this.stoppedByCap = stoppedByCap;
        this.records = new ArrayList<>(records);
        this.pageLines = new ArrayList<>(pageLines);
        this.totalPages = totalPages;
        this.textReportFile = textReportFile;
        this.jsonReportFile = jsonReportFile;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getWorldName() {
        return worldName;
    }

    public int getRadius() {
        return radius;
    }

    public boolean isNearFloorOnly() {
        return nearFloorOnly;
    }

    public int getTotalContainers() {
        return totalContainers;
    }

    public int getChestCount() {
        return chestCount;
    }

    public int getTrappedChestCount() {
        return trappedChestCount;
    }

    public int getDoubleChestCount() {
        return doubleChestCount;
    }

    public int getBarrelCount() {
        return barrelCount;
    }

    public int getNonEmptyCount() {
        return nonEmptyCount;
    }

    public int getDistinctItemGroups() {
        return distinctItemGroups;
    }

    public List<String> getTopDirectItems() {
        return Collections.unmodifiableList(topDirectItems);
    }

    public List<String> getTopNestedItems() {
        return Collections.unmodifiableList(topNestedItems);
    }

    public int getScannedChunks() {
        return scannedChunks;
    }

    public int getSkippedUnloadedChunks() {
        return skippedUnloadedChunks;
    }

    public boolean isStoppedByCap() {
        return stoppedByCap;
    }

    public List<ContainerRecord> getRecords() {
        return Collections.unmodifiableList(records);
    }

    public int getTotalPages() {
        return totalPages;
    }

    public File getTextReportFile() {
        return textReportFile;
    }

    public File getJsonReportFile() {
        return jsonReportFile;
    }

    public List<String> getPageLines(int page) {
        int pageSize = ContainerFinderManager.PAGE_SIZE;
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, pageLines.size());

        if (start < 0 || start >= pageLines.size()) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(pageLines.subList(start, end));
    }
}