package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.switches.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class SwitchesCommands {
    private final JavaPlugin plugin;
    private final LightConfigManager lightConfig;
    private final SwitchConfigManager switchConfig;
    private final LightManager lightManager;
    private SwitchManager switchManager;

    // Changed to match benches pattern
    public SwitchesCommands(JavaPlugin plugin,
                            LightConfigManager lightConfig,
                            SwitchConfigManager switchConfig) {
        this.plugin = plugin;
        this.lightConfig = lightConfig;
        this.switchConfig = switchConfig;
        this.lightManager = new LightManager(lightConfig);
        this.switchManager = new SwitchManager(switchConfig, lightConfig);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        try {
            switch (cmd.getName().toLowerCase()) {
                case "scanlights" -> handleScanLights(player);
                case "exportlights" -> handleExportLights(player, args);
                case "listlightregions" -> handleListRegions(player);
                case "checklightregion" -> handleCheckRegion(player);
                case "lightinfo" -> handleLightInfo(player);
                case "reloadsunwayswitches" -> handleReload(sender);
                default -> {
                    return false;
                }
            }
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cError: " + e.getMessage());
        } catch (Exception e) {
            player.sendMessage("§cAn unexpected error occurred!");
            plugin.getLogger().severe(e.getMessage());
        }
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("sunwayminecraft.switches.reload")) {
            sender.sendMessage("§cNo permission!");
            return true;
        }

        try {
            // Reload configs using instance methods
            lightConfig.reload();
            switchConfig.reload();

            // Reinitialize dependent components with instances
            switchManager = new SwitchManager(switchConfig, lightConfig);

            sender.sendMessage("§aReloaded configurations:");
            sender.sendMessage("§a- Light regions: " + lightConfig.getRegions().size());
            sender.sendMessage("§a- Button switches: " + switchConfig.getSwitches().size());
            return true;
        } catch (Exception e) {
            sender.sendMessage("§cReload failed: " + e.getMessage());
            return true;
        }
    }

    private void handleScanLights(Player player) {
        LightRegion region = getCurrentRegion(player);
        List<Block> lights = lightManager.scanRegion(region, player);

        player.sendMessage("§6Found " + lights.size() + " light blocks:");
        lights.forEach(b -> player.sendMessage(formatBlockLocation(b)));
    }
}
