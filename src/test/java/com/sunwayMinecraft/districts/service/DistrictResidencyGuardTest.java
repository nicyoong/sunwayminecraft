package com.sunwayMinecraft.districts.service;

import com.sunwayMinecraft.districts.config.DistrictSettingsConfig;
import com.sunwayMinecraft.districts.domain.DistrictDefinition;
import com.sunwayMinecraft.districts.domain.DistrictOwnership;
import com.sunwayMinecraft.districts.domain.DistrictType;
import com.sunwayMinecraft.districts.region.DistrictShape;
import com.sunwayMinecraft.districts.region.Region3i;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Rental guard: archived, contested and alignment access denials. */
class DistrictResidencyGuardTest {
    private DistrictAlignmentService alignmentService;
    private DistrictSettingsConfig settings;
    private DistrictResidencyGuard guard;
    private final AtomicInteger rentDenials = new AtomicInteger();

    private final Player player = mock(Player.class);

    private final DistrictDefinition archived = district("archive", DistrictType.ARCHIVED, false);
    private final DistrictDefinition contested = district("contested", DistrictType.RESIDENTIAL, true);
    private final DistrictDefinition restricted = district("restricted", DistrictType.RESIDENTIAL, false);

    private DistrictDefinition district(String id, DistrictType type, boolean contested) {
        DistrictOwnership ownership = new DistrictOwnership(null, null, null,
                List.of(), List.of(), null, false, contested);
        return new DistrictDefinition(id, id, null, "world",
                DistrictShape.cuboid(new Region3i("world", 0, 0, 0, 9, 9, 9)), true, type, 1,
                "summary", List.of(), true, 50, false, false, null, false, false, ownership);
    }

    @BeforeEach
    void setUp() {
        alignmentService = mock(DistrictAlignmentService.class);
        settings = mock(DistrictSettingsConfig.class);
        when(settings.isEnforceResidencyRules()).thenReturn(true);
        when(settings.isArchivedReadOnly()).thenReturn(true);
        when(settings.isContestedAllowAccess()).thenReturn(false);
        when(alignmentService.getDistrictById("archive")).thenReturn(archived);
        when(alignmentService.getDistrictById("contested")).thenReturn(contested);
        when(alignmentService.getDistrictById("restricted")).thenReturn(restricted);
        when(alignmentService.canPlayerAccessDistrict(any(), any())).thenReturn(true);
        when(alignmentService.getDistrictById("unknown")).thenReturn(null);
        guard = new DistrictResidencyGuard(() -> alignmentService, () -> settings,
                () -> key -> rentDenials.incrementAndGet());
    }

    @Test
    void archivedDistrictsDenyRentals() {
        assertEquals(Optional.of("This unit is inside a read-only archive district."),
                guard.checkRentalAllowed(player, "archive"));
    }

    @Test
    void contestedDistrictsPauseRentalsUnlessConfigAllows() {
        assertEquals(Optional.of("This district is contested - rentals are paused."),
                guard.checkRentalAllowed(player, "contested"));

        when(settings.isContestedAllowAccess()).thenReturn(true);
        assertEquals(Optional.empty(), guard.checkRentalAllowed(player, "contested"),
                "contested_allow_access=true must permit rentals");
    }

    @Test
    void accessDeniedPlayersCannotRentAndTheMetricIsCounted() {
        when(alignmentService.canPlayerAccessDistrict(any(), any())).thenReturn(false);
        assertEquals(Optional.of("This district is controlled by another alignment."),
                guard.checkRentalAllowed(player, "restricted"));
        assertEquals(1, rentDenials.get(), "rent denials must be counted");
    }

    @Test
    void unitsOutsideKnownDistrictsAreNotRestricted() {
        assertEquals(Optional.empty(), guard.checkRentalAllowed(player, "unknown"));
    }

    @Test
    void disabledEnforcementAllowsAllRentals() {
        when(alignmentService.canPlayerAccessDistrict(any(), any())).thenReturn(false);
        when(settings.isEnforceResidencyRules()).thenReturn(false);
        assertEquals(Optional.empty(), guard.checkRentalAllowed(player, "restricted"));
    }
}
