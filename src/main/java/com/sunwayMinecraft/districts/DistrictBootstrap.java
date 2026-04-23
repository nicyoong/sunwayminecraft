package com.sunwayMinecraft.districts;

import com.sunwayMinecraft.commands.DistrictAdminCommands;
import com.sunwayMinecraft.commands.DistrictCommands;
import com.sunwayMinecraft.districts.config.DistrictsConfigManager;
import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;

public class DistrictBootstrap {
    private final JavaPlugin plugin;
    private final DistrictManager districtManager;

    public DistrictBootstrap(JavaPlugin plugin) {
        this.plugin = plugin;
        this.districtManager = new DistrictManager(plugin, new DistrictsConfigManager(plugin));
    }

    public DistrictManager initialize() {
        districtManager.initialize();

        register("district", new DistrictCommands(districtManager));
        register("districtadmin", new DistrictAdminCommands(districtManager));

        return districtManager;
    }

    private void register(String name, CommandExecutor executor) {
        if (plugin.getCommand(name) != null) {
            plugin.getCommand(name).setExecutor(executor);
        } else {
            plugin.getLogger().warning("Command '" + name + "' not found in plugin.yml!");
        }
    }
}
