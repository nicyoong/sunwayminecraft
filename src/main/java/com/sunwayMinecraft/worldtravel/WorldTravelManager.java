package com.sunwayMinecraft.worldtravel;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class WorldTravelManager {

    private static final String MINING_WORLD_NAME = "mining";
    private static final String LIVING_WORLD_NAME = "world";

    private final JavaPlugin plugin;

    public WorldTravelManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean teleportToMining(Player player) {
        World miningWorld = Bukkit.getWorld(MINING_WORLD_NAME);

        if (miningWorld == null) {
            player.sendMessage(
                    Component.text("The Mining World is not available right now.", NamedTextColor.RED));
            plugin.getLogger().warning("Mining world '" + MINING_WORLD_NAME + "' is not loaded.");
            return false;
        }

        Location target = miningWorld.getSpawnLocation();

        if (!teleportPlayer(player, target)) {
            player.sendMessage(
                    Component.text("Could not send you to the Mining World.", NamedTextColor.RED));
            return false;
        }

        sendMiningArrivalMessage(player);
        return true;
    }

    public boolean teleportToLiving(Player player) {
        Location personalSpawn = player.getRespawnLocation();

        if (personalSpawn != null) {
            if (!teleportPlayer(player, personalSpawn)) {
                player.sendMessage(
                        Component.text("Could not send you to your personal spawn point.", NamedTextColor.RED));
                return false;
            }

            sendLivingArrivalMessage(player, true);
            return true;
        }

        World livingWorld = Bukkit.getWorld(LIVING_WORLD_NAME);

        if (livingWorld == null) {
            player.sendMessage(
                    Component.text("The Living World is not available right now.", NamedTextColor.RED));
            plugin.getLogger().warning("Living world '" + LIVING_WORLD_NAME + "' is not loaded.");
            return false;
        }

        Location fallbackSpawn = livingWorld.getSpawnLocation();

        if (!teleportPlayer(player, fallbackSpawn)) {
            player.sendMessage(
                    Component.text("Could not send you to the Living World spawn.", NamedTextColor.RED));
            return false;
        }

        sendLivingArrivalMessage(player, false);
        return true;
    }

    public boolean isMiningWorld(Player player) {
        return player.getWorld().getName().equalsIgnoreCase(MINING_WORLD_NAME);
    }

    public boolean isLivingWorld(Player player) {
        return player.getWorld().getName().equalsIgnoreCase(LIVING_WORLD_NAME);
    }

    private boolean teleportPlayer(Player player, Location target) {
        if (target == null || target.getWorld() == null) {
            return false;
        }

        return player.teleport(target);
    }

    private void sendMiningArrivalMessage(Player player) {
        player.sendMessage(
                Component.text("You have entered the ", NamedTextColor.GREEN)
                        .append(Component.text("Mining World", NamedTextColor.GOLD))
                        .append(Component.text(".", NamedTextColor.GREEN)));

        player.sendMessage(
                Component.text("This world is for resource gathering and exploration.", NamedTextColor.YELLOW));

        player.sendMessage(
                Component.text("Do not treat the Mining World as your permanent home.", NamedTextColor.YELLOW));

        player.sendMessage(
                Component.text("Use ", NamedTextColor.GRAY)
                        .append(Component.text("/lifeworld", NamedTextColor.AQUA))
                        .append(Component.text(" to return to your personal spawn or the main world spawn.", NamedTextColor.GRAY)));
    }

    private void sendLivingArrivalMessage(Player player, boolean usedPersonalSpawn) {
        if (usedPersonalSpawn) {
            player.sendMessage(
                    Component.text("You have returned to your ", NamedTextColor.GREEN)
                            .append(Component.text("personal spawn point", NamedTextColor.GOLD))
                            .append(Component.text(".", NamedTextColor.GREEN)));
        } else {
            player.sendMessage(
                    Component.text("You have returned to the ", NamedTextColor.GREEN)
                            .append(Component.text("Living World", NamedTextColor.GOLD))
                            .append(Component.text(" spawn.", NamedTextColor.GREEN)));
        }
    }
}