package com.sunwayMinecraft.residency.listener;

import com.sunwayMinecraft.residency.ResidencyManager;
import org.bukkit.scheduler.BukkitRunnable;

public class ResidencyScheduler extends BukkitRunnable {
    private final ResidencyManager manager;

    public ResidencyScheduler(ResidencyManager manager) { this.manager = manager; }

    @Override
    public void run() {
        manager.getRepository().purgeExpiredGuestAccess();
        manager.getBillingService().processDueRent();
        manager.getRepossessionService().processRepossessions();
    }
}
