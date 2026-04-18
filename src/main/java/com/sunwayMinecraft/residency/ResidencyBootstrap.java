package com.sunwayMinecraft.residency;

import com.sunwayMinecraft.residency.admin.AdminSelectionManager;
import com.sunwayMinecraft.residency.config.*;
import com.sunwayMinecraft.residency.leasing.BillingService;
import com.sunwayMinecraft.residency.listener.ResidencyBlockListener;
import com.sunwayMinecraft.residency.listener.ResidencyScheduler;
import com.sunwayMinecraft.residency.listener.UnitRegistrationToolListener;
import com.sunwayMinecraft.residency.storage.YamlResidencyRepository;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.java.JavaPlugin;

public class ResidencyBootstrap {
    private final ResidencyManager manager;

    public ResidencyBootstrap(JavaPlugin plugin, Economy economy, AdminSelectionManager selectionManager) {
        DistrictsConfigManager districts = new DistrictsConfigManager(plugin);
        BuildingsConfigManager buildings = new BuildingsConfigManager(plugin);
        UnitsConfigManager units = new UnitsConfigManager(plugin);
        PricingConfigManager pricing = new PricingConfigManager(plugin);
        PolicyConfigManager policy = new PolicyConfigManager(plugin);
        YamlResidencyRepository repository = new YamlResidencyRepository(plugin);

        this.manager = new ResidencyManager(plugin, districts, buildings, units, pricing, policy, repository);
        this.manager.initializeServices(new BillingService(manager, economy));

        plugin.getServer().getPluginManager().registerEvents(new ResidencyBlockListener(manager), plugin);
        plugin.getServer().getPluginManager().registerEvents(new UnitRegistrationToolListener(selectionManager), plugin);
        new ResidencyScheduler(manager).runTaskTimer(plugin, 20L, 1200L);
    }

    public ResidencyManager initialize() {
        manager.initialize();
        return manager;
    }
}
