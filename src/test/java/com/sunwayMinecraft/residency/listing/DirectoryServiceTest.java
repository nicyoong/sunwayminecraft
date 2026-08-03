package com.sunwayMinecraft.residency.listing;

import com.sunwayMinecraft.residency.ResidencyManager;
import com.sunwayMinecraft.residency.domain.LeaseState;
import com.sunwayMinecraft.residency.domain.ListingSettings;
import com.sunwayMinecraft.residency.domain.UnitDefinition;
import com.sunwayMinecraft.residency.domain.UnitFlags;
import com.sunwayMinecraft.residency.domain.UnitMode;
import com.sunwayMinecraft.residency.domain.UnitTenancyRecord;
import com.sunwayMinecraft.residency.domain.UnitType;
import com.sunwayMinecraft.residency.storage.ResidencyRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DirectoryServiceTest {
    @Test
    void listsOnlyVisibleVacantOrListedUnitsAndFiltersByMode() {
        ResidencyManager manager = mock(ResidencyManager.class);
        ResidencyRepository repository = mock(ResidencyRepository.class);
        UnitDefinition home = unit("home", UnitMode.RESIDENTIAL, true);
        UnitDefinition shop = unit("shop", UnitMode.COMMERCIAL, true);
        UnitDefinition hidden = unit("hidden", UnitMode.RESIDENTIAL, false);
        UnitTenancyRecord homeRecord = record("home", LeaseState.LISTED);
        UnitTenancyRecord shopRecord = record("shop", LeaseState.VACANT);
        UnitTenancyRecord hiddenRecord = record("hidden", LeaseState.VACANT);
        when(manager.getUnits()).thenReturn(Map.of("home", home, "shop", shop, "hidden", hidden));
        when(manager.getRepository()).thenReturn(repository);
        when(repository.getTenancy("home")).thenReturn(homeRecord);
        when(repository.getTenancy("shop")).thenReturn(shopRecord);
        when(repository.getTenancy("hidden")).thenReturn(hiddenRecord);
        DirectoryService directory = new DirectoryService(manager);

        assertEquals(2, directory.listAvailableUnits().size());
        assertEquals(List.of(home), directory.listAvailableByMode("residential"));
        assertEquals(List.of(shop), directory.listAvailableByMode("COMMERCIAL"));
    }

    private UnitDefinition unit(String id, UnitMode mode, boolean visible) {
        return new UnitDefinition(id, "building", "district", id, id, UnitType.APARTMENT, mode, 1,
                "address", "1", "price", "policy", 1,
                new UnitFlags(false, true, false, false, false, false),
                new ListingSettings(visible, false, List.of()), null, Map.of());
    }

    private UnitTenancyRecord record(String id, LeaseState state) {
        UnitTenancyRecord record = new UnitTenancyRecord(id);
        record.setLeaseState(state);
        return record;
    }
}
