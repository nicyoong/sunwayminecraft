package com.sunwayMinecraft.alignments.service;

import com.sunwayMinecraft.alignments.config.AlignmentProgressionConfig;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AlignmentRankServiceTest {
    @TempDir
    Path dataDirectory;

    private AlignmentRankService service;

    @BeforeEach
    void setUp() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(dataDirectory.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("AlignmentRankServiceTest"));
        doAnswer(invocation -> {
            java.io.File out = new java.io.File(dataDirectory.toFile(),
                invocation.getArgument(0, String.class));
            if (!out.exists()) {
                try (java.io.InputStream in = getClass().getClassLoader()
                        .getResourceAsStream(out.getName())) {
                    if (in != null) {
                        java.nio.file.Files.copy(in, out.toPath());
                    }
                }
            }
            return null;
        }).when(plugin).saveResource(org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyBoolean());
        AlignmentProgressionConfig config = new AlignmentProgressionConfig(plugin);
        config.load();
        service = new AlignmentRankService(config);
    }

    @Test
    void rankThresholdsMatchTheDefaultProgression() {
        assertEquals("initiate", service.resolveRank(0, "concordat_of_the_dawn").get().id());
        assertEquals("initiate", service.resolveRank(24, "concordat_of_the_dawn").get().id());
        assertEquals("associate", service.resolveRank(25, "concordat_of_the_dawn").get().id());
        assertEquals("associate", service.resolveRank(74, "concordat_of_the_dawn").get().id());
        assertEquals("fellow", service.resolveRank(75, "concordat_of_the_dawn").get().id());
        assertEquals("steward", service.resolveRank(150, "concordat_of_the_dawn").get().id());
        assertEquals("archon", service.resolveRank(300, "concordat_of_the_dawn").get().id());
    }

    @Test
    void rankIdsAreNullSafeForCacheStorage() {
        assertEquals("archon", service.resolveRankId(500, "ironclad_syndicate"));
    }

    @Test
    void meetsRankUsesTheRankThreshold() {
        assertTrue(service.meetsRank(150, "concordat_of_the_dawn", "steward"));
        assertTrue(service.meetsRank(160, "concordat_of_the_dawn", "steward"));
        assertFalse(service.meetsRank(149, "concordat_of_the_dawn", "steward"));
        assertFalse(service.meetsRank(300, "concordat_of_the_dawn", "nonexistent_rank"),
                "unknown ranks are never satisfied");
    }

    @Test
    void brokenConfigFallsBackToNoRank() {
        AlignmentRankService broken = new AlignmentRankService(mock(AlignmentProgressionConfig.class));
        assertTrue(broken.resolveRank(100, null).isEmpty(),
                "mocked empty config must resolve to no rank, not throw");
    }
}
