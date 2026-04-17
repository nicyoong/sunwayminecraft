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
        if (!economy.has(tenant, pricing.getDeposit() + pricing.getBaseRent())) return false;
        economy.withdrawPlayer(tenant, pricing.getDeposit() + pricing.getBaseRent());
        UnitTenancyRecord record = manager.getRepository().getTenancy(unit.getId());
        Instant now = Instant.now();
        record.setTenantPlayerId(tenant.getUniqueId());
        record.setDepositAmount(pricing.getDeposit());
        record.setRentAmount(pricing.getBaseRent());
        record.setBillingPeriod(pricing.getBillingPeriod());
        record.setLeaseState(LeaseState.ACTIVE);
        record.setRentState(RentState.CURRENT);
        record.setApprovalRequired(approvalRequired);
        record.setLeaseStart(now);
        record.setLastPaymentAt(now);
        record.setNextDueAt(pricing.getBillingPeriod() == BillingPeriod.WEEKLY ? now.plus(7, ChronoUnit.DAYS) : now.plus(30, ChronoUnit.DAYS));
        manager.getRepository().saveTenancy(record);
        return true;
    }

