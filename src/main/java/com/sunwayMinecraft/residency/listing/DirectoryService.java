package com.sunwayMinecraft.residency.listing;

import com.sunwayMinecraft.residency.ResidencyManager;
import com.sunwayMinecraft.residency.domain.LeaseState;
import com.sunwayMinecraft.residency.domain.UnitDefinition;
import com.sunwayMinecraft.residency.domain.UnitTenancyRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DirectoryService {
    private final ResidencyManager manager;

    public DirectoryService(ResidencyManager manager) { this.manager = manager; }

    public List<UnitDefinition> listAvailableUnits() {
        List<UnitDefinition> out = new ArrayList<>();
        for (UnitDefinition unit : manager.getUnits().values()) {
            if (!unit.getListingSettings().isVisible()) continue;
            UnitTenancyRecord tenancy = manager.getRepository().getTenancy(unit.getId());
            if (tenancy.getLeaseState() == LeaseState.VACANT || tenancy.getLeaseState() == LeaseState.LISTED) out.add(unit);
        }
        return out;
    }

    public List<UnitDefinition> listAvailableByMode(String mode) {
        return listAvailableUnits().stream().filter(u -> u.getMode().name().equalsIgnoreCase(mode)).collect(Collectors.toList());
    }
}
