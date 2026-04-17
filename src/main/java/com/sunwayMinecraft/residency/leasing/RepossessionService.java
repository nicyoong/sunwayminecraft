package com.sunwayMinecraft.residency.leasing;

import com.sunwayMinecraft.residency.ResidencyManager;
import com.sunwayMinecraft.residency.domain.*;

import java.time.Instant;

public class RepossessionService {
    private final ResidencyManager manager;

    public RepossessionService(ResidencyManager manager) { this.manager = manager; }

    public void processRepossessions() {
        Instant now = Instant.now();
        for (UnitTenancyRecord record : manager.getRepository().getAllTenancies()) {
            if (record.getLeaseState() != LeaseState.ARREARS_RESTRICTED || record.getGraceEnd() == null) continue;
            UnitDefinition unit = manager.getUnits().get(record.getUnitId().toLowerCase());
            if (unit == null) continue;
            PolicyProfile policy = manager.getPolicyProfile(unit);
            if (!policy.isRepossessionEnabled()) continue;
            if (now.isAfter(record.getGraceEnd().plusSeconds(86400L))) {
                record.setLeaseState(unit.getFlags().isEscrowOnRepossession() ? LeaseState.ESCROW_OPEN : LeaseState.REPOSSESSED);
                record.setLeaseEnd(now);
                record.setTenantPlayerId(null);
                record.getManagerIds().clear();
                record.setRentState(RentState.CLOSED);
                manager.getRepository().saveTenancy(record);
                if (unit.getFlags().isEscrowOnRepossession()) manager.getRepository().saveEscrow(new EscrowRecord(unit.getId(), now, "Automatic repossession", "OPEN"));
            }
        }
    }
}
