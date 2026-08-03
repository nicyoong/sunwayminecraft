package com.sunwayMinecraft.containerfinder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContainerReportWriterTest {
    @TempDir
    Path directory;

    @Test
    void textAndJsonReportsContainStructuredSortedScanData() throws Exception {
        ContainerScanCache cache = cache();
        File text = directory.resolve("scan.txt").toFile();
        File json = directory.resolve("scan.json").toFile();

        ContainerReportWriter.writeTextReport(cache, text);
        ContainerReportWriter.writeJsonReport(cache, json);

        String textContent = Files.readString(text.toPath());
        String jsonContent = Files.readString(json.toPath());
        assertTrue(textContent.contains("Container Scan Report"));
        assertTrue(textContent.contains("Chest @ world X:1 Y:64 Z:2"));
        assertTrue(textContent.indexOf("Diamond x5") < textContent.indexOf("Apple x2"));
        assertTrue(jsonContent.contains("\"totalContainers\":1"));
        assertTrue(jsonContent.contains("\"type\":\"Chest\""));
        assertTrue(jsonContent.indexOf("\"Diamond\"") < jsonContent.indexOf("\"Apple\""));
    }

    @Test
    void cachePagesAreOneBasedAndKeepRecordsImmutable() {
        ContainerScanCache cache = cache();

        assertEquals(2, cache.getTotalPages());
        assertEquals(10, cache.getPageLines(1).size());
        assertEquals(List.of("eleventh"), cache.getPageLines(2));
        assertTrue(cache.getPageLines(0).isEmpty());
        assertTrue(cache.getPageLines(3).isEmpty());
    }

    private ContainerScanCache cache() {
        ContainerRecord record = new ContainerRecord("Chest", "world", 1, 64, 2,
                Map.of("diamond", 5L, "apple", 2L), Map.of("diamond", "Diamond", "apple", "Apple"),
                Map.of(), Map.of());
        return new ContainerScanCache("2026-01-01T00:00:00Z", "world", 32, false,
                1, 1, 0, 0, 0, 0, 1, 2,
                List.of("Diamond x5", "Apple x2"), List.of(), 4, 1, false,
                List.of(record), List.of("first", "second", "third", "fourth", "fifth", "sixth", "seventh",
                        "eighth", "ninth", "tenth", "eleventh"), 2, null, null);
    }
}
