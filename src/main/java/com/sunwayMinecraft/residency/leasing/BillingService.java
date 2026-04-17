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

