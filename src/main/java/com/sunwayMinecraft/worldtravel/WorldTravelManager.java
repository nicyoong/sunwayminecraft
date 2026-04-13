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
            plugin.getLogger().warning("Mining world '" + MINING_WORLD_NAME + "' was not found.");
            return false;
        }

        Location destination = miningWorld.getSpawnLocation();

        if (!safeTeleport(player, destination)) {
            player.sendMessage(
                    Component.text("Failed to teleport you to the Mining World.", NamedTextColor.RED));
            return false;
        }

        player.sendMessage(
                Component.text("Teleported to the ", NamedTextColor.GREEN)
                        .append(Component.text("Mining World", NamedTextColor.GOLD))
                        .append(Component.text(".", NamedTextColor.GREEN)));

        player.sendMessage(
                Component.text("This world is for resource gathering.", NamedTextColor.YELLOW));

        return true;
    }

    public boolean teleportToLiving(Player player) {
        Location personalSpawn = player.getRespawnLocation();

        if (personalSpawn != null) {
            if (!safeTeleport(player, personalSpawn)) {
                player.sendMessage(
                        Component.text("Failed to teleport you to your spawn point.", NamedTextColor.RED));
                return false;
            }

            player.sendMessage(
                    Component.text("Teleported to your ", NamedTextColor.GREEN)
                            .append(Component.text("personal spawn point", NamedTextColor.GOLD))
                            .append(Component.text(".", NamedTextColor.GREEN)));
            return true;
        }

        World livingWorld = Bukkit.getWorld(LIVING_WORLD_NAME);

        if (livingWorld == null) {
            player.sendMessage(
                    Component.text(
                            "You do not have a personal spawn point, and the Living World is unavailable.",
                            NamedTextColor.RED));
            plugin.getLogger().warning("Living world '" + LIVING_WORLD_NAME + "' was not found.");
            return false;
        }

        Location fallback = livingWorld.getSpawnLocation();

        if (!safeTeleport(player, fallback)) {
            player.sendMessage(
                    Component.text(
                            "You do not have a personal spawn point, and fallback teleport failed.",
                            NamedTextColor.RED));
            return false;
        }

        player.sendMessage(
                Component.text("You do not have a personal spawn point set.", NamedTextColor.YELLOW));

        player.sendMessage(
                Component.text("Teleported to the ", NamedTextColor.GREEN)
                        .append(Component.text("Living World spawn", NamedTextColor.GOLD))
                        .append(Component.text(" instead.", NamedTextColor.GREEN)));

        return true;
    }

    private boolean safeTeleport(Player player, Location destination) {
        if (destination == null || destination.getWorld() == null) {
            return false;
        }

        try {
            return player.teleport(destination);
        } catch (Exception ex) {
            plugin.getLogger().warning(
                    "Teleport failed for player " + player.getName() + ": " + ex.getMessage());
            return false;
        }
    }
}