package com.sunwayMinecraft.districts;

import com.sunwayMinecraft.commands.DistrictAdminCommands;
import com.sunwayMinecraft.commands.DistrictCommands;
import com.sunwayMinecraft.districts.config.DistrictSettingsConfig;
import com.sunwayMinecraft.districts.config.DistrictsConfigManager;
import com.sunwayMinecraft.districts.listener.DistrictEnterListener;
import com.sunwayMinecraft.districts.listener.DistrictProtectionListener;
import com.sunwayMinecraft.districts.region.DistrictLocationResolver;
import com.sunwayMinecraft.districts.service.DistrictAlignmentService;
import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class DistrictBootstrap {
    private final JavaPlugin plugin;
    private final DistrictsConfigManager configManager;
    private final DistrictManager districtManager;

    private final DistrictAlignmentService alignmentService;
    private final DistrictSettingsConfig settings;

    public DistrictBootstrap(JavaPlugin plugin, Function<java.util.UUID, Optional<String>> alignmentResolver,
                             Supplier<Consumer<String>> metricsSupplier) {
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
    }

    public DistrictSettingsConfig getSettings() {
        return settings;
    }

    public DistrictManager initialize() {
        districtManager.initialize();

        register("district", new DistrictCommands(districtManager, alignmentService,
                new com.sunwayMinecraft.commands.DistrictAdminSubCommands(districtManager, configManager)));
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
