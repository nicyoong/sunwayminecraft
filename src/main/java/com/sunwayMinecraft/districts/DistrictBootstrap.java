package com.sunwayMinecraft.districts;

import com.sunwayMinecraft.commands.DistrictAdminCommands;
import com.sunwayMinecraft.commands.DistrictCommands;
import com.sunwayMinecraft.districts.config.DistrictControlSettingsConfig;
import com.sunwayMinecraft.districts.config.DistrictSettingsConfig;
import com.sunwayMinecraft.districts.config.DistrictsConfigManager;
import com.sunwayMinecraft.districts.listener.DistrictEnterListener;
import com.sunwayMinecraft.districts.listener.DistrictProtectionListener;
import com.sunwayMinecraft.districts.region.DistrictLocationResolver;
import com.sunwayMinecraft.districts.persistence.DistrictControlRepository;
import com.sunwayMinecraft.districts.service.DistrictAlignmentService;
import com.sunwayMinecraft.districts.service.DistrictControlService;
import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.milkbowl.vault.economy.Economy;

public class DistrictBootstrap {
    private final JavaPlugin plugin;
    private final DistrictsConfigManager configManager;
    private final DistrictManager districtManager;

    private final DistrictAlignmentService alignmentService;
    private final DistrictSettingsConfig settings;
    private final DistrictControlService controlService;
    private final DistrictControlRepository controlRepository;

    private final DistrictControlSettingsConfig controlSettings;

    public DistrictBootstrap(JavaPlugin plugin, Function<java.util.UUID, Optional<String>> alignmentResolver,
                             Supplier<Consumer<String>> metricsSupplier, Supplier<Economy> economySupplier) {
        this.plugin = plugin;
        this.configManager = new DistrictsConfigManager(plugin);
        this.settings = new DistrictSettingsConfig(plugin);
        this.settings.load();
        this.districtManager = new DistrictManager(plugin, configManager);
        // No alignment feature on this branch yet: the resolver decides who is
        // aligned. Wire the alignment membership cache here when alignments land.
        this.alignmentService = new DistrictAlignmentService(configManager, alignmentResolver);
        DistrictLocationResolver locationResolver = new DistrictLocationResolver(configManager);
        new DistrictProtectionListener(plugin, locationResolver, alignmentService, settings,
                () -> {
                    Consumer<String> metrics = metricsSupplier.get();
                    return metrics == null ? null : metrics::accept;
                }).register();
        new DistrictEnterListener(plugin, locationResolver, settings).register();

        this.controlSettings = new DistrictControlSettingsConfig(plugin);
        this.controlSettings.load();
        this.controlRepository = new DistrictControlRepository(plugin);
        this.controlService = new DistrictControlService(plugin, configManager, locationResolver,
                controlSettings, controlRepository, alignmentResolver, economySupplier,
                null, metricsSupplier);
        controlService.load();
        controlService.start();
    }

    public DistrictControlSettingsConfig getControlSettings() {
        return controlSettings;
    }

    public DistrictControlRepository getControlRepository() {
        return controlRepository;
    }

    public DistrictControlService getControlService() {
        return controlService;
    }

    public DistrictSettingsConfig getSettings() {
        return settings;
    }

    public DistrictManager initialize() {
        districtManager.initialize();

        register("district", new DistrictCommands(districtManager, alignmentService,
                new com.sunwayMinecraft.commands.DistrictAdminSubCommands(districtManager, configManager),
                new com.sunwayMinecraft.commands.DistrictControlCommands(controlService, controlSettings)));
        register("districtadmin", new DistrictAdminCommands(districtManager));

        return districtManager;
    }

    public DistrictAlignmentService getAlignmentService() {
        return alignmentService;
    }

    private void register(String name, CommandExecutor executor) {
        if (plugin.getCommand(name) != null) {
            plugin.getCommand(name).setExecutor(executor);
        } else {
            plugin.getLogger().warning("Command '" + name + "' not found in plugin.yml!");
        }
    }
}
