package com.sunwayMinecraft.residency.domain;

public class PricingProfile {
    private final String id;
    private final BillingPeriod billingPeriod;
    private final double baseRent;
    private final double deposit;
    private final boolean lateFeeEnabled;
    private final double lateFee;

    public PricingProfile(String id, BillingPeriod billingPeriod, double baseRent, double deposit,
                          boolean lateFeeEnabled, double lateFee) {
        this.id = id;
        this.billingPeriod = billingPeriod;
        this.baseRent = baseRent;
        this.deposit = deposit;
        this.lateFeeEnabled = lateFeeEnabled;
        this.lateFee = lateFee;
    }

    public String getId() { return id; }
    public BillingPeriod getBillingPeriod() { return billingPeriod; }
    public double getBaseRent() { return baseRent; }
    public double getDeposit() { return deposit; }
    public boolean isLateFeeEnabled() { return lateFeeEnabled; }
    public double getLateFee() { return lateFee; }
}
