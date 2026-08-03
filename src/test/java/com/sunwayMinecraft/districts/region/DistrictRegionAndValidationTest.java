package com.sunwayMinecraft.districts.region;

import com.sunwayMinecraft.districts.config.DistrictsConfigManager;
import com.sunwayMinecraft.districts.domain.ApprovalBias;
import com.sunwayMinecraft.districts.domain.DistrictDefinition;
import com.sunwayMinecraft.districts.domain.DistrictType;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DistrictRegionAndValidationTest {
    private ServerMock server;
    private World world;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void regionNormalizesCornersUsesInclusiveBoundsAndDetectsOnlySameWorldOverlaps() {
        Region3i region = new Region3i("world", 5, 8, 9, 1, 2, 3);

        assertEquals(1, region.getMinX());
        assertEquals(5, region.getMaxX());
        assertEquals(5L * 7 * 7, region.getVolume());
        assertTrue(region.contains(new Location(world, 1, 2, 3)));
        assertTrue(region.contains(new Location(world, 5, 8, 9)));
        assertFalse(region.contains(new Location(world, 6, 8, 9)));
        assertTrue(region.overlapsVolume(new Region3i("WORLD", 5, 8, 9, 6, 9, 10)));
        assertFalse(region.overlapsVolume(new Region3i("other", 1, 2, 3, 5, 8, 9)));
    }

    @Test
    void resolverSkipsDisabledDistrictsAndReturnsNullOutsideConfiguredRegions() {
        DistrictsConfigManager config = mock(DistrictsConfigManager.class);
        DistrictDefinition disabled = district("disabled", new Region3i("world", 0, 0, 0, 10, 10, 10), false, 1, "summary");
        DistrictDefinition enabled = district("enabled", new Region3i("world", 20, 0, 20, 30, 10, 30), true, 1, "summary");
        when(config.getDistricts()).thenReturn(List.of(disabled, enabled));
        DistrictResolver resolver = new DistrictResolver(config);

        assertNull(resolver.resolve(new Location(world, 1, 1, 1)));
        assertEquals(enabled, resolver.resolve(new Location(world, 20, 1, 20)));
        assertNull(resolver.resolve(null));
    }

    @Test
    void validationReportsDefinitionAndOverlapFailuresButIgnoresDisabledOverlap() {
        DistrictsConfigManager config = mock(DistrictsConfigManager.class);
        DistrictDefinition invalid = district("invalid", new Region3i("missing", 0, 0, 0, 1000, 1000, 1000), true, 0, "");
        DistrictDefinition overlap = district("overlap", new Region3i("missing", 1, 1, 1, 2, 2, 2), true, 1, "summary");
        DistrictDefinition disabled = district("disabled", new Region3i("missing", 1, 1, 1, 2, 2, 2), false, 1, "summary");
        when(config.getDistricts()).thenReturn(List.of(invalid, overlap, disabled));

        List<String> errors = new DistrictValidationService(mock(JavaPlugin.class), config).validateAll();

        assertTrue(errors.stream().anyMatch(s -> s.contains("empty display name")));
        assertTrue(errors.stream().anyMatch(s -> s.contains("missing a public summary")));
        assertTrue(errors.stream().anyMatch(s -> s.contains("missing world")));
        assertTrue(errors.stream().anyMatch(s -> s.contains("prestige tier")));
        assertTrue(errors.stream().anyMatch(s -> s.contains("exceeds max volume")));
        assertEquals(1, errors.stream().filter(s -> s.startsWith("Districts '")).count());
    }

    private DistrictDefinition district(String id, Region3i region, boolean enabled, int prestige, String summary) {
        return new DistrictDefinition(id, id.equals("invalid") ? "" : id, null, region.getWorld(), region, enabled,
                DistrictType.RESIDENTIAL, prestige, summary, List.of(), true, 0, false, false,
                ApprovalBias.STANDARD, false, false);
    }
}
