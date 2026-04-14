package com.sunwayMinecraft.worldtravel;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class MiningWorldEvacuationManager {

    private final JavaPlugin plugin;
    private final WorldTravelManager worldTravelManager;

    private BukkitTask activeEvacuationTask;
    private int secondsRemaining;
    private boolean evacuationRunning;

    public MiningWorldEvacuationManager(JavaPlugin plugin, WorldTravelManager worldTravelManager) {
        this.plugin = plugin;
        this.worldTravelManager = worldTravelManager;
    }

    public boolean isEvacuationRunning() {
        return evacuationRunning;
    }

    public int getSecondsRemaining() {
        return secondsRemaining;
    }

    public boolean startEvacuation(int durationSeconds) {
        if (durationSeconds <= 0) {
            return false;
        }

        if (evacuationRunning) {
            return false;
        }

        World miningWorld = worldTravelManager.getMiningWorld();
        if (miningWorld == null) {
            plugin.getLogger().warning("Cannot start mining evacuation because mining world is not loaded.");
            return false;
        }

        evacuationRunning = true;
        secondsRemaining = durationSeconds;
        worldTravelManager.setMiningWorldState(MiningWorldState.RESET_PENDING);

        worldTravelManager.broadcastMiningStateChange(
                Component.text("[Mining World] ", NamedTextColor.DARK_GRAY)
                        .append(Component.text(
                                "Evacuation has started. The Mining World will close in " + durationSeconds + " seconds.",
                                NamedTextColor.GOLD)));

        activeEvacuationTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (secondsRemaining <= 0) {
                    finishEvacuation();
                    cancel();
                    return;
                }

                if (shouldBroadcast(secondsRemaining)) {
                    worldTravelManager.broadcastMiningStateChange(
                            Component.text("[Mining World] ", NamedTextColor.DARK_GRAY)
                                    .append(Component.text(
                                            "Evacuate now. Mining World closes in " + secondsRemaining + " seconds.",
                                            NamedTextColor.RED)));
                }

                secondsRemaining--;
            }
        }.runTaskTimer(plugin, 20L, 20L);

        return true;
    }

    public boolean cancelEvacuation() {
        if (!evacuationRunning) {
            return false;
        }

        if (activeEvacuationTask != null) {
            activeEvacuationTask.cancel();
            activeEvacuationTask = null;
        }

        evacuationRunning = false;
        secondsRemaining = 0;
        worldTravelManager.setMiningWorldState(MiningWorldState.OPEN);

        worldTravelManager.broadcastMiningStateChange(
                Component.text("[Mining World] ", NamedTextColor.DARK_GRAY)
                        .append(Component.text("Evacuation cancelled. The Mining World is OPEN again.", NamedTextColor.GREEN)));

        return true;
    }

    public void shutdown() {
        if (activeEvacuationTask != null) {
            activeEvacuationTask.cancel();
            activeEvacuationTask = null;
        }
        evacuationRunning = false;
        secondsRemaining = 0;
    }

    private void finishEvacuation() {
        World miningWorld = worldTravelManager.getMiningWorld();
        if (miningWorld != null) {
            for (Player player : miningWorld.getPlayers()) {
                player.sendMessage(
                        Component.text("The Mining World is closing. You are being returned now.", NamedTextColor.RED));
                worldTravelManager.teleportPlayerOutOfMining(player);
            }
        }

        worldTravelManager.setMiningWorldState(MiningWorldState.LOCKED);
        evacuationRunning = false;
        secondsRemaining = 0;
        activeEvacuationTask = null;

        worldTravelManager.broadcastMiningStateChange(
                Component.text("[Mining World] ", NamedTextColor.DARK_GRAY)
                        .append(Component.text("Evacuation complete. The Mining World is now LOCKED.", NamedTextColor.RED)));
    }

    private boolean shouldBroadcast(int seconds) {
        return seconds == 300
                || seconds == 180
                || seconds == 120
                || seconds == 60
                || seconds == 30
                || seconds == 15
                || seconds == 10
                || seconds <= 5;
    }
}