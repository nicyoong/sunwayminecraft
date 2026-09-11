package com.sunwayMinecraft.districts.service;

import com.sunwayMinecraft.districts.domain.DistrictDefinition;
import com.sunwayMinecraft.districts.domain.DistrictOwnership;
import com.sunwayMinecraft.districts.domain.DistrictType;
import com.sunwayMinecraft.districts.region.DistrictShape;
import com.sunwayMinecraft.districts.region.Region3i;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Access, build, rent and trade decisions for alignment districts. */
class DistrictAlignmentServiceTest {
    private static final DistrictShape REGION = DistrictShape.cuboid(new Region3i("world", 0, 0, 0, 9, 9, 9));

    private final UUID playerUuid = UUID.randomUUID();
    private final Player player = playerWith(false, "azure_hearth");

    private final DistrictAlignmentService service = new DistrictAlignmentService(
            mock(com.sunwayMinecraft.districts.config.DistrictsConfigManager.class),
            uuid -> uuid.equals(playerUuid) ? Optional.of("azure_hearth") : Optional.empty());

    private Player playerWith(boolean bypass, String alignmentId) {
        Player mock = mock(Player.class);
        when(mock.getUniqueId()).thenReturn(playerUuid);
        when(mock.hasPermission("sunway.district.admin.bypass")).thenReturn(bypass);
        return mock;
    }

    private DistrictDefinition district(DistrictType type, DistrictOwnership ownership) {
        return new DistrictDefinition("test", "Test", null, "world", REGION, true,
                type, 1, "summary", List.of(), true, 50, false, false, null, false, false,
                ownership);
    }

    private final DistrictDefinition neutral = district(DistrictType.CIVIC, DistrictOwnership.neutral());
    private final DistrictDefinition whitelist = district(DistrictType.CAMPUS,
            new DistrictOwnership("taylors", "concordat_of_the_dawn", "azure_hearth",
                    List.of("azure_hearth"), List.of(), null, false, false));
    private final DistrictDefinition denying = district(DistrictType.RESIDENTIAL,
            new DistrictOwnership(null, null, null,
                    List.of("azure_hearth", "spirewrights"), List.of("spirewrights"),
                    null, false, false));
    private final DistrictDefinition archived = district(DistrictType.ARCHIVED,
            DistrictOwnership.neutral());
    private final DistrictDefinition disabled = new DistrictDefinition(
            "closed", "Closed", null, "world", REGION, false, DistrictType.RESIDENTIAL,
            1, "summary", List.of(), true, 50, false, false, null, false, false,
            DistrictOwnership.neutral());

    @Test
    void alignmentAccessFollowsTheRuleAndUnalignedFollowsTheWhitelist() {
        assertTrue(service.canAlignmentAccessDistrict("azure_hearth", whitelist));
        assertFalse(service.canAlignmentAccessDistrict("spirewrights", whitelist));
        assertTrue(service.canAlignmentAccessDistrict("azure_hearth", denying));
        assertFalse(service.canAlignmentAccessDistrict("spirewrights", denying),
                "denied wins over allowed");
        assertTrue(service.canAlignmentAccessDistrict(null, neutral),
                "neutral districts allow anyone including the unaligned");
    }

    @Test
    void playerAccessUsesTheResolvedAlignmentOrUnalignedFallback() {
        assertTrue(service.canPlayerAccessDistrict(player, whitelist),
                "azure_hearth member on an azure_hearth whitelist");

        Player unaligned = mock(Player.class);
        when(unaligned.getUniqueId()).thenReturn(UUID.randomUUID());
        when(unaligned.hasPermission("sunway.district.admin.bypass")).thenReturn(false);
        assertFalse(service.canPlayerAccessDistrict(unaligned, whitelist),
                "unaligned players are denied on whitelist districts");
        assertTrue(service.canPlayerAccessDistrict(unaligned, neutral));
    }

    @Test
    void archivedDistrictsAreReadOnlyForEveryone() {
        assertFalse(service.canPlayerBuildInDistrict(player, archived));
        assertFalse(service.canPlayerRentInDistrict(player, archived));
        assertFalse(service.canPlayerTradeInDistrict(player, archived));

        Player admin = playerWith(true, "azure_hearth");
        assertTrue(service.canPlayerBuildInDistrict(admin, archived),
                "the admin bypass unlocks archived districts");
        assertTrue(service.canPlayerRentInDistrict(admin, archived));
        assertTrue(service.canPlayerTradeInDistrict(admin, archived));
    }

    @Test
    void accessDeniedBlocksBuildRentAndTrade() {
        Player spirewright = mock(Player.class);
        when(spirewright.getUniqueId()).thenReturn(UUID.randomUUID());
        when(spirewright.hasPermission("sunway.district.admin.bypass")).thenReturn(false);

        assertFalse(service.canPlayerBuildInDistrict(spirewright, denying));
        assertFalse(service.canPlayerRentInDistrict(spirewright, denying));
        assertFalse(service.canPlayerTradeInDistrict(spirewright, denying));

        assertTrue(service.canPlayerBuildInDistrict(player, denying));
        assertTrue(service.canPlayerRentInDistrict(player, denying));
        assertTrue(service.canPlayerTradeInDistrict(player, denying));
    }

    @Test
    void disabledDistrictsCannotBeBuiltIn() {
        assertFalse(service.canPlayerBuildInDistrict(player, disabled));
        assertTrue(service.canPlayerRentInDistrict(player, disabled),
                "rent rules stay with the residency system; disabled only blocks building");
    }

    @Test
    void adminBypassOverridesAccessEverywhere() {
        Player admin = playerWith(true, null);
        assertTrue(service.canPlayerAccessDistrict(admin, whitelist));
        assertTrue(service.canPlayerBuildInDistrict(admin, whitelist));
        assertTrue(service.canPlayerRentInDistrict(admin, whitelist));
        assertTrue(service.canPlayerTradeInDistrict(admin, whitelist));
    }
}
