package com.sunwayMinecraft.residency.access;

import com.sunwayMinecraft.residency.ResidencyManager;
import com.sunwayMinecraft.residency.domain.*;
import com.sunwayMinecraft.residency.storage.ResidencyRepository;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.junit.jupiter.api.AfterEach;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PremisesAccessServiceTest {

    private ServerMock server;
    private PremisesAccessService accessService;

    @Mock
    private ResidencyManager manager;

    @Mock
    private ResidencyRepository repository;

    private PlayerMock player;
    private Location location;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        MockitoAnnotations.openMocks(this);
        when(manager.getRepository()).thenReturn(repository);
        accessService = new PremisesAccessService(manager);
        
        player = server.addPlayer();
        location = new Location(server.addSimpleWorld("world"), 100, 64, 100);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private UnitDefinition createMockUnit(String id) {
        UnitFlags flags = new UnitFlags(false, true, true, true, false, false);
        return new UnitDefinition(id, "building1", "district1", "Unit 1", "U1",
                UnitType.APARTMENT, UnitMode.RESIDENTIAL, 1, "Address", "1",
                "pricing1", "policy1", 2, flags, null, null, null);
    }

    private UnitTenancyRecord createTenancy(String unitId) {
        return new UnitTenancyRecord(unitId);
    }

    @Test
    void testStaffOverride() {
        UnitDefinition unit = createMockUnit("unit1");
        when(manager.getUnitAt(any(Location.class))).thenReturn(unit);
        // PlayerMock doesn't support permissions easily without setting them
        player.addAttachment(MockBukkit.createMockPlugin()).setPermission("sunway.residency.override", true);

        AccessDecision decision = accessService.check(player, location, ActionType.BREAK_BLOCK);

        assertTrue(decision.isAllowed());
        assertEquals(RoleType.STAFF_OVERRIDE, decision.getResolvedRole());
    }

    @Test
    void testTenantAccess() {
        UUID playerId = player.getUniqueId();
        UnitDefinition unit = createMockUnit("unit1");
        UnitTenancyRecord tenancy = createTenancy("unit1");
        tenancy.setTenantPlayerId(playerId);
        tenancy.setLeaseState(LeaseState.ACTIVE);

        when(manager.getUnitAt(any(Location.class))).thenReturn(unit);
        when(repository.getTenancy("unit1")).thenReturn(tenancy);

        AccessDecision decision = accessService.check(player, location, ActionType.BREAK_BLOCK);

        assertTrue(decision.isAllowed());
        assertEquals(RoleType.TENANT, decision.getResolvedRole());
    }

    @Test
    void testManagerAccess() {
        UUID playerId = player.getUniqueId();
        UnitDefinition unit = createMockUnit("unit1");
        UnitTenancyRecord tenancy = createTenancy("unit1");
        tenancy.getManagerIds().add(playerId);
        tenancy.setLeaseState(LeaseState.ACTIVE);

        when(manager.getUnitAt(any(Location.class))).thenReturn(unit);
        when(repository.getTenancy("unit1")).thenReturn(tenancy);

        // Manager can enter
        AccessDecision enterDecision = accessService.check(player, location, ActionType.ENTER);
        assertTrue(enterDecision.isAllowed());
        assertEquals(RoleType.MANAGER, enterDecision.getResolvedRole());

        // Manager cannot break block
        AccessDecision breakDecision = accessService.check(player, location, ActionType.BREAK_BLOCK);
        assertFalse(breakDecision.isAllowed());
        assertEquals(RoleType.MANAGER, breakDecision.getResolvedRole());
        assertTrue(breakDecision.getDenialReason().contains("Managers may not edit blocks"));
    }

    @Test
    void testGuestAccess() {
        UUID playerId = player.getUniqueId();
        UnitDefinition unit = createMockUnit("unit1");
        UnitTenancyRecord tenancy = createTenancy("unit1");
        tenancy.setLeaseState(LeaseState.ACTIVE);
        
        GuestAccessGrant grant = mock(GuestAccessGrant.class);
        when(grant.getPlayerId()).thenReturn(playerId);
        when(grant.isActive(any())).thenReturn(true);

        when(manager.getUnitAt(any(Location.class))).thenReturn(unit);
        when(repository.getTenancy("unit1")).thenReturn(tenancy);
        when(repository.getGuestAccess("unit1")).thenReturn(List.of(grant));

        // Guest can enter
        assertTrue(accessService.check(player, location, ActionType.ENTER).isAllowed());
        // Guest can open door
        assertTrue(accessService.check(player, location, ActionType.OPEN_DOOR).isAllowed());
        // Guest cannot break block
        assertFalse(accessService.check(player, location, ActionType.BREAK_BLOCK).isAllowed());
    }

    @Test
    void testPublicAccess() {
        UnitFlags flags = new UnitFlags(true, true, true, true, false, true); // publicEntry=true, publicContainerAccess=true
        UnitDefinition unit = new UnitDefinition("unit1", "b1", "d1", "U1", "U1",
                UnitType.APARTMENT, UnitMode.RESIDENTIAL, 1, "Addr", "1",
                "pr1", "pol1", 2, flags, null, null, null);
        UnitTenancyRecord tenancy = createTenancy("unit1");
        tenancy.setLeaseState(LeaseState.ACTIVE);

        PolicyProfile policy = new PolicyProfile("pol1", UnitMode.RESIDENTIAL, 7, true, true, false, false);

        when(manager.getUnitAt(any(Location.class))).thenReturn(unit);
        when(repository.getTenancy("unit1")).thenReturn(tenancy);
        when(manager.getPolicyProfile(unit)).thenReturn(policy);

        // Public can enter because publicEntry is true
        assertTrue(accessService.check(player, location, ActionType.ENTER).isAllowed());
        
        // Public can use container because publicContainerAccess is true
        assertTrue(accessService.check(player, location, ActionType.USE_CONTAINER).isAllowed());

        // Public cannot break block
        assertFalse(accessService.check(player, location, ActionType.BREAK_BLOCK).isAllowed());
    }

    @Test
    void testDenialStates() {
        UUID playerId = player.getUniqueId();
        UnitDefinition unit = createMockUnit("unit1");
        UnitTenancyRecord tenancy = createTenancy("unit1");
        tenancy.setTenantPlayerId(playerId);

        when(manager.getUnitAt(any(Location.class))).thenReturn(unit);
        when(repository.getTenancy("unit1")).thenReturn(tenancy);

        // Repossessed
        tenancy.setLeaseState(LeaseState.REPOSSESSED);
        assertFalse(accessService.check(player, location, ActionType.ENTER).isAllowed());

        // Escrow Open
        tenancy.setLeaseState(LeaseState.ESCROW_OPEN);
        assertFalse(accessService.check(player, location, ActionType.ENTER).isAllowed());

        // Arrears Restricted - Tenant should be denied if not staff
        tenancy.setLeaseState(LeaseState.ARREARS_RESTRICTED);
        
        // Let's test non-tenant denial for Arrears Restricted
        PlayerMock otherPlayer = server.addPlayer();
        assertFalse(accessService.check(otherPlayer, location, ActionType.ENTER).isAllowed());
    }

    @Test
    void testUnmanagedLocation() {
        when(manager.getUnitAt(any(Location.class))).thenReturn(null);
        AccessDecision decision = accessService.check(player, location, ActionType.BREAK_BLOCK);
        assertTrue(decision.isAllowed());
        assertNull(decision.getUnit());
    }
}
