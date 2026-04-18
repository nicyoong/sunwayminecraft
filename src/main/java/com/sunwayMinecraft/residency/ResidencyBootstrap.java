package com.sunwayMinecraft.residency;

import com.sunwayMinecraft.commands.ResidencyAdminCommands;
import com.sunwayMinecraft.commands.ResidencyCommands;
import com.sunwayMinecraft.commands.StorefrontCommands;
import com.sunwayMinecraft.residency.admin.AdminSelectionManager;
import com.sunwayMinecraft.residency.config.*;
import com.sunwayMinecraft.residency.leasing.BillingService;
import com.sunwayMinecraft.residency.listener.*;
import com.sunwayMinecraft.residency.storage.YamlResidencyRepository;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.PluginCommand;
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
        this.manager.initializeServices(new BillingService(manager, economy));
    }

    public ResidencyManager initialize() {
        manager.initialize();

        AdminSelectionManager selectionManager = new AdminSelectionManager();

        ResidencyCommands residencyCommands = new ResidencyCommands(manager);
        StorefrontCommands storefrontCommands = new StorefrontCommands(manager);
        ResidencyAdminCommands adminCommands = new ResidencyAdminCommands(manager, selectionManager);

        register("residency", residencyCommands);
        register("storefront", storefrontCommands);
        register("resadmin", adminCommands);

        plugin.getServer().getPluginManager().registerEvents(new ResidencyBlockListener(manager), plugin);
        plugin.getServer().getPluginManager().registerEvents(new UnitRegistrationToolListener(selectionManager), plugin);

        new ResidencyScheduler(manager).runTaskTimer(plugin, 20L, 1200L);
        return manager;
    }

    private void register(String name, org.bukkit.command.TabExecutor executor) {
        PluginCommand command = plugin.getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        } else {
            plugin.getLogger().warning("Command '" + name + "' not found in plugin.yml!");
        }
    }
}
