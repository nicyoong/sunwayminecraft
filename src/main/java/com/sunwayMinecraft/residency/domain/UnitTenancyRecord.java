package com.sunwayMinecraft.residency.domain;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class UnitTenancyRecord {
    private final String unitId;
    private LeaseState leaseState;
    private RentState rentState;
    private UUID tenantPlayerId;
    private final Set<UUID> managerIds = new HashSet<>();
    private Instant leaseStart;
    private Instant leaseEnd;
    private Instant graceEnd;
    private double depositAmount;
    private double rentAmount;
    private BillingPeriod billingPeriod;
    private double arrearsAmount;
    private Instant lastPaymentAt;
    private Instant nextDueAt;
    private boolean approvalRequired;

    public UnitTenancyRecord(String unitId) {
        this.unitId = unitId;
        this.leaseState = LeaseState.VACANT;
        this.rentState = RentState.CLOSED;
    }

    public String getUnitId() { return unitId; }
    public LeaseState getLeaseState() { return leaseState; }
    public void setLeaseState(LeaseState leaseState) { this.leaseState = leaseState; }
    public RentState getRentState() { return rentState; }
    public void setRentState(RentState rentState) { this.rentState = rentState; }
    public UUID getTenantPlayerId() { return tenantPlayerId; }
    public void setTenantPlayerId(UUID tenantPlayerId) { this.tenantPlayerId = tenantPlayerId; }
    public Set<UUID> getManagerIds() { return managerIds; }
    public Instant getLeaseStart() { return leaseStart; }
    public void setLeaseStart(Instant leaseStart) { this.leaseStart = leaseStart; }
    public Instant getLeaseEnd() { return leaseEnd; }
    public void setLeaseEnd(Instant leaseEnd) { this.leaseEnd = leaseEnd; }
    public Instant getGraceEnd() { return graceEnd; }
    public void setGraceEnd(Instant graceEnd) { this.graceEnd = graceEnd; }
    public double getDepositAmount() { return depositAmount; }
    public void setDepositAmount(double depositAmount) { this.depositAmount = depositAmount; }
    public double getRentAmount() { return rentAmount; }
    public void setRentAmount(double rentAmount) { this.rentAmount = rentAmount; }
    public BillingPeriod getBillingPeriod() { return billingPeriod; }
    public void setBillingPeriod(BillingPeriod billingPeriod) { this.billingPeriod = billingPeriod; }
    public double getArrearsAmount() { return arrearsAmount; }
    public void setArrearsAmount(double arrearsAmount) { this.arrearsAmount = arrearsAmount; }
    public Instant getLastPaymentAt() { return lastPaymentAt; }
    public void setLastPaymentAt(Instant lastPaymentAt) { this.lastPaymentAt = lastPaymentAt; }
    public Instant getNextDueAt() { return nextDueAt; }
    public void setNextDueAt(Instant nextDueAt) { this.nextDueAt = nextDueAt; }
    public boolean isApprovalRequired() { return approvalRequired; }
    public void setApprovalRequired(boolean approvalRequired) { this.approvalRequired = approvalRequired; }
}
