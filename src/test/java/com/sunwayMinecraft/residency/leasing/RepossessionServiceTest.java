package com.sunwayMinecraft.residency.leasing;

import com.sunwayMinecraft.residency.ResidencyManager;
import com.sunwayMinecraft.residency.domain.LeaseState;
import com.sunwayMinecraft.residency.domain.ListingSettings;
import com.sunwayMinecraft.residency.domain.PolicyProfile;
import com.sunwayMinecraft.residency.domain.RentState;
import com.sunwayMinecraft.residency.domain.UnitDefinition;
import com.sunwayMinecraft.residency.domain.UnitFlags;
import com.sunwayMinecraft.residency.domain.UnitMode;
import com.sunwayMinecraft.residency.domain.UnitTenancyRecord;
import com.sunwayMinecraft.residency.domain.UnitType;
import com.sunwayMinecraft.residency.storage.ResidencyRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RepossessionServiceTest {
    @Test
    void overdueArrearsLeaseIsRepossessedAndCreatesEscrowWhenConfigured() {
        ResidencyManager manager = mock(ResidencyManager.class);
        ResidencyRepository repository = mock(ResidencyRepository.class);
        UnitDefinition unit = unit(true);
        UnitTenancyRecord record = new UnitTenancyRecord("unit");
        record.setTenantPlayerId(UUID.randomUUID());
        record.setLeaseState(LeaseState.ARREARS_RESTRICTED);
        record.setGraceEnd(Instant.now().minusSeconds(2 * 86400));
        record.setRentState(RentState.IN_ARREARS);
        when(manager.getRepository()).thenReturn(repository);
        when(repository.getAllTenancies()).thenReturn(List.of(record));
        when(manager.getUnits()).thenReturn(Map.of("unit", unit));
        when(manager.getPolicyProfile(unit)).thenReturn(new PolicyProfile("policy", UnitMode.RESIDENTIAL, 1, true, true, false, false));

        new RepossessionService(manager).processRepossessions();

        assertEquals(LeaseState.ESCROW_OPEN, record.getLeaseState());
        assertEquals(RentState.CLOSED, record.getRentState());
        assertNull(record.getTenantPlayerId());
        verify(repository).saveTenancy(record);
        verify(repository).saveEscrow(argThat(escrow -> escrow.getUnitId().equals("unit")
                && escrow.getReason().equals("Automatic repossession") && escrow.getStatus().equals("OPEN")));
    }

    @Test
    void repossessionDoesNotRunBeforeThePostGraceDayOrWhenPolicyDisablesIt() {
        ResidencyManager manager = mock(ResidencyManager.class);
        ResidencyRepository repository = mock(ResidencyRepository.class);
        UnitDefinition unit = unit(false);
        UnitTenancyRecord record = new UnitTenancyRecord("unit");
        record.setLeaseState(LeaseState.ARREARS_RESTRICTED);
        record.setGraceEnd(Instant.now().minusSeconds(60));
        when(manager.getRepository()).thenReturn(repository);
        when(repository.getAllTenancies()).thenReturn(List.of(record));
        when(manager.getUnits()).thenReturn(Map.of("unit", unit));
        when(manager.getPolicyProfile(unit)).thenReturn(new PolicyProfile("policy", UnitMode.RESIDENTIAL, 1, false, true, false, false));

        new RepossessionService(manager).processRepossessions();

        assertEquals(LeaseState.ARREARS_RESTRICTED, record.getLeaseState());
        verify(repository, never()).saveTenancy(record);
        verify(repository, never()).saveEscrow(org.mockito.ArgumentMatchers.any());
    }

    private UnitDefinition unit(boolean escrow) {
        return new UnitDefinition("unit", "building", "district", "Unit", "U1", UnitType.APARTMENT,
                UnitMode.RESIDENTIAL, 1, "Address", "1", "price", "policy", 1,
                new UnitFlags(false, true, false, escrow, false, false),
                new ListingSettings(true, false, List.of()), null, Map.of());
    }
}
