package com.sunwayMinecraft.districts.region;

import com.sunwayMinecraft.districts.config.DistrictsConfigManager;
import com.sunwayMinecraft.districts.domain.DistrictDefinition;
import com.sunwayMinecraft.districts.domain.DistrictType;
import com.sunwayMinecraft.districts.domain.DistrictOwnership;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Shape containment and the overlapping-district priority rules. */
class DistrictLocationResolverTest {
    private ServerMock server;
    private World world;
    private DistrictsConfigManager configManager;
    private DistrictLocationResolver resolver;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        configManager = mock(DistrictsConfigManager.class);
        resolver = new DistrictLocationResolver(configManager);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private DistrictDefinition district(String id, DistrictType type, int minX, int maxX) {
        return new DistrictDefinition(id, id, null, "world",
                DistrictShape.cuboid(new Region3i("world", minX, 0, 0, maxX, 20, 20)), true,
                type, 1, "summary", java.util.List.of(), true, 50, false, false, null,
                false, false, DistrictOwnership.neutral());
    }

    private void withDistricts(DistrictDefinition... districts) {
        when(configManager.getDistricts()).thenReturn(List.of(districts));
    }

    @Test
    void cuboidShapesContainOnlyLocationsInsideTheRegion() {
        withDistricts(district("box", DistrictType.CIVIC, 0, 10));
        Location inside = new Location(world, 5, 10, 10);
        Location outside = new Location(world, 15, 10, 10);

        assertTrue(resolver.isInsideDistrict(inside, configManager.getDistricts().iterator().next()));
        assertEquals("box", resolver.getDistrictAt(inside).getId());
        org.junit.jupiter.api.Assertions.assertNull(resolver.getDistrictAt(outside));
    }

    @Test
    void overlappingDistrictsPreferArchivedThenSanctuaryThenSmaller() {
        DistrictDefinition archived = district("archive", DistrictType.ARCHIVED, 0, 30);
        DistrictDefinition sanctuary = district("sanctuary", DistrictType.SANCTUARY, 5, 25);
        DistrictDefinition small = district("small", DistrictType.CIVIC, 10, 14);
        DistrictDefinition large = district("large", DistrictType.CIVIC, 8, 16);
        withDistricts(archived, sanctuary, small, large);
        Location middle = new Location(world, 12, 10, 10);

        assertEquals("archive", resolver.getDistrictAt(middle).getId(),
                "archived districts win over everything");
        withDistricts(sanctuary, small, large);
        assertEquals("sanctuary", resolver.getDistrictAt(middle).getId(),
                "sanctuary wins when no archived district applies");
        withDistricts(small, large);
        assertEquals("small", resolver.getDistrictAt(middle).getId(),
                "the smaller (more specific) district wins within the same priority");
    }

    @Test
    void pointRadiusShapesDetectPresenceByDistance() {
        DistrictDefinition zone = new DistrictDefinition("zone", "Zone", null, "world",
                DistrictShape.pointRadius("world", 0, 0, 0, 15), true, DistrictType.TRANSIT, 1,
                "summary", java.util.List.of(), true, 50, false, false, null, false, false,
                DistrictOwnership.neutral());
        withDistricts(zone);

        assertEquals("zone", resolver.getDistrictAt(new Location(world, 10, 0, 10)).getId(),
                "10 blocks from the center is inside a radius-15 sphere");
        org.junit.jupiter.api.Assertions.assertNull(resolver.getDistrictAt(new Location(world, 30, 0, 0)),
                "30 blocks from the center is outside the sphere");
    }

    @Test
    void getDistrictsAtReturnsEveryContainingDistrict() {
        withDistricts(district("outer", DistrictType.CIVIC, 0, 30),
                      district("inner", DistrictType.CIVIC, 5, 15));
        List<DistrictDefinition> matches = resolver.getDistrictsAt(new Location(world, 10, 10, 10));
        assertEquals(2, matches.size(), "overlapping districts are all reported");
    }

    @Test
    void disabledDistrictsAreNeverResolved() {
        DistrictDefinition disabled = new DistrictDefinition("off", "Off", null, "world",
                DistrictShape.cuboid(new Region3i("world", 0, 0, 0, 30, 20, 20)), false,
                DistrictType.CIVIC, 1, "summary", java.util.List.of(), true, 50, false, false,
                null, false, false, DistrictOwnership.neutral());
        withDistricts(disabled);
        org.junit.jupiter.api.Assertions.assertNull(resolver.getDistrictAt(new Location(world, 10, 10, 10)));
    }
}
