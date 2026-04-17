package com.sunwayMinecraft.residency;

import com.sunwayMinecraft.commands.ResidencyAdminCommands;
import com.sunwayMinecraft.commands.ResidencyCommands;
import com.sunwayMinecraft.commands.StorefrontCommands;
import com.sunwayMinecraft.residency.config.*;
import com.sunwayMinecraft.residency.leasing.BillingService;
import com.sunwayMinecraft.residency.listener.ResidencyBlockListener;
import com.sunwayMinecraft.residency.listener.ResidencyScheduler;
import com.sunwayMinecraft.residency.storage.YamlResidencyRepository;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;

public class ResidencyBootstrap {
    private final JavaPlugin plugin;
    private final ResidencyManager manager;

    public ResidencyBootstrap(JavaPlugin plugin, Economy economy) {
        this.plugin = plugin;

        DistrictsConfigManager districts = new DistrictsConfigManager(plugin);
        BuildingsConfigManager buildings = new BuildingsConfigManager(plugin);
        UnitsConfigManager units = new UnitsConfigManager(plugin);
        PricingConfigManager pricing = new PricingConfigManager(plugin);
        PolicyConfigManager policy = new PolicyConfigManager(plugin);
        YamlResidencyRepository repository = new YamlResidencyRepository(plugin);

        this.manager = new ResidencyManager(plugin, districts, buildings, units, pricing, policy, repository);

        BillingService billingService = new BillingService(manager, economy);
        this.manager.initializeServices(billingService);
    }

    public ResidencyManager initialize() {
        manager.initialize();

        plugin.getServer().getPluginManager().registerEvents(new ResidencyBlockListener(manager), plugin);
        new ResidencyScheduler(manager).runTaskTimer(plugin, 20L, 1200L);

        register("residency", new ResidencyCommands(manager));
        register("storefront", new StorefrontCommands(manager));
        register("resadmin", new ResidencyAdminCommands(manager));

        return manager;
    }

    private void register(String name, CommandExecutor executor) {
        if (plugin.getCommand(name) != null) {
            plugin.getCommand(name).setExecutor(executor);
        } else {
            plugin.getLogger().warning("Command '" + name + "' not found in plugin.yml!");
        }
    }
}
