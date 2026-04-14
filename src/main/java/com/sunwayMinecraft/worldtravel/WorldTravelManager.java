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
    private static final String LIFE_WORLD_NAME = "world";

    private final JavaPlugin plugin;
    private final WorldTravelConfigManager configManager;

    private MiningWorldState miningWorldState = MiningWorldState.OPEN;

    public WorldTravelManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configManager = new WorldTravelConfigManager(plugin);
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public WorldTravelConfigManager getConfigManager() {
        return configManager;
    }

    public void loadState() {
        miningWorldState = configManager.loadMiningWorldState();
        plugin.getLogger().info("Loaded Mining World state: " + miningWorldState);
    }

    public void saveState() {
        configManager.saveMiningWorldState(miningWorldState);
    }

    public MiningWorldState getMiningWorldState() {
        return miningWorldState;
    }

    public void setMiningWorldState(MiningWorldState miningWorldState) {
        this.miningWorldState = miningWorldState;
        saveState();
    }

    public boolean isMiningOpen() {
        return miningWorldState == MiningWorldState.OPEN
                || miningWorldState == MiningWorldState.RESET_PENDING;
    }

    public boolean isMiningLocked() {
        return miningWorldState == MiningWorldState.LOCKED;
    }

    public boolean isMiningResetPending() {
        return miningWorldState == MiningWorldState.RESET_PENDING;
    }

    public boolean teleportToMining(Player player) {
        if (isMiningLocked()) {
            player.sendMessage(
                    Component.text("The Mining World is currently locked.", NamedTextColor.RED));
            player.sendMessage(
                    Component.text("Please use ", NamedTextColor.GRAY)
                            .append(Component.text("/lifeworld", NamedTextColor.AQUA))
                            .append(Component.text(" and wait for staff to reopen it.", NamedTextColor.GRAY)));
            return false;
        }

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

    public boolean teleportToLifeWorld(Player player) {
        Location personalSpawn = player.getRespawnLocation();

        if (personalSpawn != null) {
            if (!teleportPlayer(player, personalSpawn)) {
                player.sendMessage(
                        Component.text("Could not send you to your personal spawn point.", NamedTextColor.RED));
                return false;
            }

            sendLifeWorldArrivalMessage(player, true);
            return true;
        }

        World lifeWorld = Bukkit.getWorld(LIFE_WORLD_NAME);
        if (lifeWorld == null) {
            player.sendMessage(
                    Component.text("The Life World is not available right now.", NamedTextColor.RED));
            plugin.getLogger().warning("Life world '" + LIFE_WORLD_NAME + "' is not loaded.");
            return false;
        }

        Location fallbackSpawn = lifeWorld.getSpawnLocation();
        if (!teleportPlayer(player, fallbackSpawn)) {
            player.sendMessage(
                    Component.text("Could not send you to the Life World spawn.", NamedTextColor.RED));
            return false;
        }

        sendLifeWorldArrivalMessage(player, false);
        return true;
    }

    public boolean teleportPlayerOutOfMining(Player player) {
        return teleportToLifeWorld(player);
    }

    public boolean isMiningWorld(Player player) {
        return player.getWorld().getName().equalsIgnoreCase(MINING_WORLD_NAME);
    }

    public boolean isLifeWorld(Player player) {
        return player.getWorld().getName().equalsIgnoreCase(LIFE_WORLD_NAME);
    }

    public World getMiningWorld() {
        return Bukkit.getWorld(MINING_WORLD_NAME);
    }

    public Component buildMiningInfoMessage() {
        Component header = Component.text("Mining World Information", NamedTextColor.GOLD);

        Component line1 =
                Component.text("• ", NamedTextColor.DARK_GRAY)
                        .append(Component.text("Purpose: ", NamedTextColor.YELLOW))
                        .append(Component.text("resource gathering and exploration", NamedTextColor.WHITE));

        Component line2 =
                Component.text("• ", NamedTextColor.DARK_GRAY)
                        .append(Component.text("Do not use it for: ", NamedTextColor.YELLOW))
                        .append(Component.text("permanent builds or long-term storage", NamedTextColor.WHITE));

        Component line3 =
                Component.text("• ", NamedTextColor.DARK_GRAY)
                        .append(Component.text("Travel there: ", NamedTextColor.YELLOW))
                        .append(Component.text("/mineworld", NamedTextColor.AQUA));

        Component line4 =
                Component.text("• ", NamedTextColor.DARK_GRAY)
                        .append(Component.text("Return home: ", NamedTextColor.YELLOW))
                        .append(Component.text("/lifeworld", NamedTextColor.AQUA));

        Component line5 =
                Component.text("• ", NamedTextColor.DARK_GRAY)
                        .append(Component.text("Current state: ", NamedTextColor.YELLOW))
                        .append(getStateDisplayComponent());

        return header
                .append(Component.newline())
                .append(line1)
                .append(Component.newline())
                .append(line2)
                .append(Component.newline())
                .append(line3)
                .append(Component.newline())
                .append(line4)
                .append(Component.newline())
                .append(line5);
    }

    public Component getStateDisplayComponent() {
        return switch (miningWorldState) {
            case OPEN -> Component.text("OPEN", NamedTextColor.GREEN);
            case RESET_PENDING -> Component.text("RESET PENDING", NamedTextColor.GOLD);
            case LOCKED -> Component.text("LOCKED", NamedTextColor.RED);
        };
    }

    public void broadcastMiningStateChange(Component message) {
        Bukkit.getServer().broadcast(message);
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
                Component.text("Do not treat this world as your permanent home.", NamedTextColor.YELLOW));

        if (isMiningResetPending()) {
            player.sendMessage(
                    Component.text("Warning: the Mining World is marked as reset pending.", NamedTextColor.GOLD));
        }

        player.sendMessage(
                Component.text("Use ", NamedTextColor.GRAY)
                        .append(Component.text("/lifeworld", NamedTextColor.AQUA))
                        .append(Component.text(" to return to your respawn point or the main world spawn.", NamedTextColor.GRAY)));
    }

    private void sendLifeWorldArrivalMessage(Player player, boolean usedPersonalSpawn) {
        if (usedPersonalSpawn) {
            player.sendMessage(
                    Component.text("You have returned to your ", NamedTextColor.GREEN)
                            .append(Component.text("personal spawn point", NamedTextColor.GOLD))
                            .append(Component.text(".", NamedTextColor.GREEN)));
        } else {
            player.sendMessage(
                    Component.text("You have returned to the ", NamedTextColor.GREEN)
                            .append(Component.text("Life World", NamedTextColor.GOLD))
                            .append(Component.text(" spawn.", NamedTextColor.GREEN)));
        }
    }
}