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

    private void handleExportLights(Player player, String[] args) {
        if (args.length < 1) throw new IllegalArgumentException("§cUsage: /exportlights <filename>");

        LightRegion region = getCurrentRegion(player);
        List<Block> lights = lightManager.scanRegion(region, player);
        File outputFile = new File(plugin.getDataFolder(), args[0]);

        try (FileWriter writer = new FileWriter(outputFile)) {
            for (Block block : lights) {
                writer.write(block.getType() + "," +
                        block.getX() + "," +
                        block.getY() + "," +
                        block.getZ() + "\n");
            }
            player.sendMessage("§aSuccessfully exported " + lights.size() +
                    " lights to " + outputFile.getName());
        } catch (IOException e) {
            throw new IllegalArgumentException("§cFailed to write file: " + e.getMessage());
        }
    }

    private void handleListRegions(Player player) {
        Map<String, LightRegion> regions = lightConfig.getRegions();
        if (regions.isEmpty()) {
            player.sendMessage("§eNo light regions defined.");
            return;
        }

        player.sendMessage("§6Defined light regions:");
        regions.forEach((name, region) -> {
            if (name != null) {
                player.sendMessage("§a- " + name);
            }
        });
    }

    private void handleCheckRegion(Player player) {
        Location loc = player.getLocation();
        Optional<LightRegion> region = lightConfig.getRegions().values().stream()
                .filter(r -> r.contains(loc))
                .findFirst();

        if (region.isPresent()) {
            player.sendMessage("§aYou're in light region: §6" + region.get().name());
        } else {
            player.sendMessage("§eYou're not in any light region.");
        }
    }

    private void handleLightInfo(Player player) {
        Block target = player.getTargetBlockExact(5);
        if (target == null || !LightManager.isLightBlock(target.getType())) {
            throw new IllegalArgumentException("§cLook at a light block (Sea Lantern, Glowstone, or Jack-o-Lantern)");
        }

        Location blockLoc = target.getLocation();
        World currentWorld = player.getWorld();

        // Get regions with proper world validation
        List<LightRegion> regions = lightConfig.getRegions().values().stream()
                .filter(r -> r.world().getName().equals(currentWorld.getName()))
                .filter(r -> r.contains(blockLoc))
                .collect(Collectors.toList());

        // Get switches with precise location comparison
        List<ButtonSwitch> switches = switchConfig.getSwitches().values().stream()
                .filter(s -> s.lightLocations().stream()
                        .anyMatch(loc ->
                                loc != null &&
                                        loc.getWorld() != null &&
                                        loc.getWorld().getName().equals(currentWorld.getName()) &&
                                        loc.getBlockX() == blockLoc.getBlockX() &&
                                        loc.getBlockY() == blockLoc.getBlockY() &&
                                        loc.getBlockZ() == blockLoc.getBlockZ()
                        ))
                .collect(Collectors.toList());
        player.sendMessage("§6=== Light Block Info ===");
        player.sendMessage(formatBlockLocation(target));

        if (!regions.isEmpty()) {
            player.sendMessage("§aBelongs to regions:");
            regions.forEach(r -> player.sendMessage("§b- " + r.name()));
        } else {
            player.sendMessage("§cNot part of any light region!");
        }

        if (!switches.isEmpty()) {
            player.sendMessage("§aControlled by buttons:");
            switches.forEach(s -> player.sendMessage("§b- " + formatLocation(s.buttonLocation())));
        } else {
            player.sendMessage("§cNot linked to any switches!");
        }
    }

    private LightRegion getCurrentRegion(Player player) {
        Location loc = player.getLocation();
        List<LightRegion> regions = lightConfig.getRegions().values().stream()
                .filter(r -> r.contains(loc))
                .collect(Collectors.toList());

        if (regions.isEmpty()) {
            throw new IllegalArgumentException("§cYou're not in any light region!");
        }
        if (regions.size() > 1) {
            throw new IllegalArgumentException("§cYou're in multiple regions! Please move to a unique region.");
        }
        return regions.get(0);
    }
}
