package com.sunwayMinecraft.districts;

import com.sunwayMinecraft.commands.DistrictAdminCommands;
import com.sunwayMinecraft.commands.DistrictCommands;
import com.sunwayMinecraft.districts.config.DistrictsConfigManager;
import com.sunwayMinecraft.districts.service.DistrictAlignmentService;
import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

public class DistrictBootstrap {
    private final JavaPlugin plugin;
    private final DistrictManager districtManager;

    private final DistrictAlignmentService alignmentService;

    public DistrictBootstrap(JavaPlugin plugin) {
        this.plugin = plugin;
        DistrictsConfigManager configManager = new DistrictsConfigManager(plugin);
        this.districtManager = new DistrictManager(plugin, configManager);
        // No alignment feature on this branch yet: every player resolves as
        // unaligned. Wire the alignment cache here when alignments land.
        this.alignmentService = new DistrictAlignmentService(configManager, uuid -> Optional.empty());
    }

    public DistrictManager initialize() {
        districtManager.initialize();

        register("district", new DistrictCommands(districtManager, alignmentService));
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
