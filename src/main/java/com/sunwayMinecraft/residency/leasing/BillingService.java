package com.sunwayMinecraft.residency.leasing;

import com.sunwayMinecraft.residency.ResidencyManager;
import com.sunwayMinecraft.residency.domain.*;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class BillingService {
    private final ResidencyManager manager;
    private final Economy economy;

    public BillingService(ResidencyManager manager, Economy economy) {
        this.manager = manager;
        this.economy = economy;
    }

    public boolean startLease(UnitDefinition unit, OfflinePlayer tenant, boolean approvalRequired) {
        PricingProfile pricing = manager.getPricingProfile(unit);
        if (pricing == null || economy == null) return false;

        double upfront = pricing.getDeposit() + pricing.getBaseRent();
        if (!economy.has(tenant, upfront)) return false;

        economy.withdrawPlayer(tenant, upfront);

        UnitTenancyRecord record = manager.getRepository().getTenancy(unit.getId());
        record.setTenantPlayerId(tenant.getUniqueId());
        record.setDepositAmount(pricing.getDeposit());
        record.setRentAmount(pricing.getBaseRent());
        record.setBillingPeriod(pricing.getBillingPeriod());
        record.setLeaseState(LeaseState.ACTIVE);
        record.setRentState(RentState.CURRENT);
        record.setApprovalRequired(approvalRequired);

        Instant now = Instant.now();
        record.setLeaseStart(now);
        record.setLastPaymentAt(now);
        record.setNextDueAt(nextDue(now, pricing.getBillingPeriod()));
        manager.getRepository().saveTenancy(record);
        return true;
    }

    public boolean payRent(UnitDefinition unit, OfflinePlayer tenant) {
        UnitTenancyRecord record = manager.getRepository().getTenancy(unit.getId());
        if (record.getTenantPlayerId() == null || !record.getTenantPlayerId().equals(tenant.getUniqueId())) return false;
        if (economy == null || !economy.has(tenant, record.getRentAmount())) return false;

        economy.withdrawPlayer(tenant, record.getRentAmount());

        Instant now = Instant.now();
        record.setLastPaymentAt(now);
        record.setNextDueAt(nextDue(now, record.getBillingPeriod()));
        record.setArrearsAmount(0.0);
        record.setRentState(RentState.CURRENT);

        if (record.getLeaseState() == LeaseState.GRACE || record.getLeaseState() == LeaseState.ARREARS_RESTRICTED) {
            record.setLeaseState(LeaseState.ACTIVE);
            record.setGraceEnd(null);
        }

        manager.getRepository().saveTenancy(record);
        return true;
    }

    public boolean terminateLease(UnitDefinition unit, OfflinePlayer tenant) {
        UnitTenancyRecord record = manager.getRepository().getTenancy(unit.getId());
        if (record.getTenantPlayerId() == null || !record.getTenantPlayerId().equals(tenant.getUniqueId())) {
            return false;
        }

        // Simple V1 rule: refund deposit on voluntary termination if economy exists.
        if (economy != null && record.getDepositAmount() > 0) {
            economy.depositPlayer(tenant, record.getDepositAmount());
        }

        record.setTenantPlayerId(null);
        record.getManagerIds().clear();
        record.setLeaseEnd(Instant.now());
        record.setGraceEnd(null);
        record.setArrearsAmount(0.0);
        record.setApprovalRequired(false);
        record.setLeaseState(unit.getListingSettings().isVisible() ? LeaseState.LISTED : LeaseState.VACANT);
        record.setRentState(RentState.CLOSED);
        manager.getRepository().saveTenancy(record);

        return true;
    }

    public void processDueRent() {
        Instant now = Instant.now();
        for (UnitTenancyRecord record : manager.getRepository().getAllTenancies()) {
            if (record.getLeaseState() != LeaseState.ACTIVE
                    && record.getLeaseState() != LeaseState.GRACE
                    && record.getLeaseState() != LeaseState.ARREARS_RESTRICTED) {
                continue;
            }

            if (record.getTenantPlayerId() == null || record.getNextDueAt() == null || now.isBefore(record.getNextDueAt())) {
                continue;
            }

            OfflinePlayer tenant = manager.getPlugin().getServer().getOfflinePlayer(record.getTenantPlayerId());
            if (economy != null && economy.has(tenant, record.getRentAmount())) {
                economy.withdrawPlayer(tenant, record.getRentAmount());
                record.setLastPaymentAt(now);
                record.setNextDueAt(nextDue(now, record.getBillingPeriod()));
                record.setRentState(RentState.CURRENT);
                record.setLeaseState(LeaseState.ACTIVE);
                record.setGraceEnd(null);
            } else {
                UnitDefinition unit = manager.getUnits().get(record.getUnitId().toLowerCase());
                PolicyProfile policy = manager.getPolicyProfile(unit);
                if (record.getLeaseState() == LeaseState.ACTIVE) {
                    record.setLeaseState(LeaseState.GRACE);
                    record.setRentState(RentState.IN_GRACE);
                    record.setGraceEnd(now.plus(policy.getRentGraceDays(), ChronoUnit.DAYS));
                    record.setArrearsAmount(record.getArrearsAmount() + record.getRentAmount());
                } else if (record.getLeaseState() == LeaseState.GRACE
                        && record.getGraceEnd() != null
                        && !now.isBefore(record.getGraceEnd())) {
                    record.setLeaseState(LeaseState.ARREARS_RESTRICTED);
                    record.setRentState(RentState.IN_ARREARS);
                }
            }

            manager.getRepository().saveTenancy(record);
        }
    }

    private Instant nextDue(Instant from, BillingPeriod period) {
        return period == BillingPeriod.WEEKLY
                ? from.plus(7, ChronoUnit.DAYS)
                : from.plus(30, ChronoUnit.DAYS);
    }
}
