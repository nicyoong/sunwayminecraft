package com.sunwayMinecraft.residency.leasing;

import com.sunwayMinecraft.residency.ResidencyManager;
import com.sunwayMinecraft.residency.domain.BillingPeriod;
import com.sunwayMinecraft.residency.domain.LeaseState;
import com.sunwayMinecraft.residency.domain.ListingSettings;
import com.sunwayMinecraft.residency.domain.PolicyProfile;
import com.sunwayMinecraft.residency.domain.PricingProfile;
import com.sunwayMinecraft.residency.domain.RentState;
import com.sunwayMinecraft.residency.domain.UnitDefinition;
import com.sunwayMinecraft.residency.domain.UnitFlags;
import com.sunwayMinecraft.residency.domain.UnitMode;
import com.sunwayMinecraft.residency.domain.UnitTenancyRecord;
import com.sunwayMinecraft.residency.domain.UnitType;
import com.sunwayMinecraft.residency.storage.ResidencyRepository;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BillingServiceTest {
    private ResidencyManager manager;
    private ResidencyRepository repository;
    private Economy economy;
    private OfflinePlayer tenant;
    private UnitDefinition unit;
    private PricingProfile pricing;
    private UnitTenancyRecord record;
    private BillingService billing;

    @BeforeEach
    void setUp() {
        manager = mock(ResidencyManager.class);
        repository = mock(ResidencyRepository.class);
        economy = mock(Economy.class);
        tenant = mock(OfflinePlayer.class);
        when(tenant.getUniqueId()).thenReturn(UUID.randomUUID());
        unit = unit(true);
        pricing = new PricingProfile("standard", BillingPeriod.MONTHLY, 100, 25, false, 0);
        record = new UnitTenancyRecord(unit.getId());
        when(manager.getRepository()).thenReturn(repository);
        when(manager.getPricingProfile(unit)).thenReturn(pricing);
        when(repository.getTenancy(unit.getId())).thenReturn(record);
        billing = new BillingService(manager, economy);
    }

    @Test
    void startLeaseChargesDepositAndRentAndCreatesAnActiveLease() {
        when(economy.has(tenant, 125)).thenReturn(true);

        assertTrue(billing.startLease(unit, tenant, true));

        verify(economy).withdrawPlayer(tenant, 125);
        verify(repository).saveTenancy(record);
        assertEquals(tenant.getUniqueId(), record.getTenantPlayerId());
        assertEquals(LeaseState.ACTIVE, record.getLeaseState());
        assertEquals(RentState.CURRENT, record.getRentState());
        assertEquals(25, record.getDepositAmount());
        assertEquals(100, record.getRentAmount());
        assertEquals(BillingPeriod.MONTHLY, record.getBillingPeriod());
        assertTrue(record.isApprovalRequired());
        assertNotNull(record.getNextDueAt());
    }

    @Test
    void startLeaseRejectsMissingPricingEconomyOrFundsWithoutMutatingTenancy() {
        when(economy.has(tenant, 125)).thenReturn(false);

        assertFalse(billing.startLease(unit, tenant, false));

        verify(economy, never()).withdrawPlayer(tenant, 125);
        verify(repository, never()).saveTenancy(record);
        assertNull(record.getTenantPlayerId());

        when(manager.getPricingProfile(unit)).thenReturn(null);
        assertFalse(billing.startLease(unit, tenant, false));
    }

    @Test
    void payRentRequiresTheCurrentTenantAndRestoresRestrictedLease() {
        record.setTenantPlayerId(tenant.getUniqueId());
        record.setRentAmount(100);
        record.setBillingPeriod(BillingPeriod.WEEKLY);
        record.setLeaseState(LeaseState.ARREARS_RESTRICTED);
        record.setRentState(RentState.IN_ARREARS);
        record.setArrearsAmount(100);
        when(economy.has(tenant, 100)).thenReturn(true);

        assertTrue(billing.payRent(unit, tenant));

        verify(economy).withdrawPlayer(tenant, 100);
        verify(repository).saveTenancy(record);
        assertEquals(LeaseState.ACTIVE, record.getLeaseState());
        assertEquals(RentState.CURRENT, record.getRentState());
        assertEquals(0, record.getArrearsAmount());
        assertNull(record.getGraceEnd());
        assertTrue(record.getNextDueAt().isAfter(Instant.now().plusSeconds(6 * 86400)));
    }

    @Test
    void terminateLeaseRefundsDepositAndReturnsVisibleUnitsToListing() {
        record.setTenantPlayerId(tenant.getUniqueId());
        record.setDepositAmount(25);
        record.getManagerIds().add(UUID.randomUUID());

        assertTrue(billing.terminateLease(unit, tenant));

        verify(economy).depositPlayer(tenant, 25);
        verify(repository).saveTenancy(record);
        assertNull(record.getTenantPlayerId());
        assertTrue(record.getManagerIds().isEmpty());
        assertEquals(LeaseState.LISTED, record.getLeaseState());
        assertEquals(RentState.CLOSED, record.getRentState());
    }

    @Test
    void dueRentMovesLeaseFromActiveToGraceThenToArrearsWhenFundsRemainUnavailable() {
        record.setTenantPlayerId(tenant.getUniqueId());
        record.setLeaseState(LeaseState.ACTIVE);
        record.setRentAmount(100);
        record.setNextDueAt(Instant.now().minusSeconds(1));
        when(repository.getAllTenancies()).thenReturn(List.of(record));
        when(manager.getUnits()).thenReturn(Map.of(unit.getId(), unit));
        when(manager.getPolicyProfile(unit)).thenReturn(new PolicyProfile("policy", UnitMode.RESIDENTIAL, 1, true, true, false, false));
        JavaPlugin plugin = mock(JavaPlugin.class);
        Server server = mock(Server.class);
        when(manager.getPlugin()).thenReturn(plugin);
        when(plugin.getServer()).thenReturn(server);
        when(server.getOfflinePlayer(tenant.getUniqueId())).thenReturn(tenant);
        when(economy.has(tenant, 100)).thenReturn(false);

        billing.processDueRent();
        assertEquals(LeaseState.GRACE, record.getLeaseState());
        assertEquals(RentState.IN_GRACE, record.getRentState());
        assertEquals(100, record.getArrearsAmount());

        record.setGraceEnd(Instant.now().minusSeconds(1));
        billing.processDueRent();
        assertEquals(LeaseState.ARREARS_RESTRICTED, record.getLeaseState());
        assertEquals(RentState.IN_ARREARS, record.getRentState());
        verify(repository, org.mockito.Mockito.atLeast(2)).saveTenancy(record);
    }

    private UnitDefinition unit(boolean visible) {
        return new UnitDefinition("unit", "building", "district", "Unit", "U1", UnitType.APARTMENT,
                UnitMode.RESIDENTIAL, 1, "Address", "1", "price", "policy", 1,
                new UnitFlags(false, true, false, true, false, false),
                new ListingSettings(visible, false, List.of()), null, Map.of());
    }
}
