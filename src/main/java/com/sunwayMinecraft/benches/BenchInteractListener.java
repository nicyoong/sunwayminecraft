package com.sunwayMinecraft.benches;

import com.sunwayMinecraft.SunwayMinecraft;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BenchInteractListener implements Listener {
    private final SunwayMinecraft plugin;
    private final RegionManager regionManager;
    private final EffectApplier effectApplier;
    private final Set<UUID> cooldowns = new HashSet<>();

    public BenchInteractListener(SunwayMinecraft plugin, RegionManager regionManager) {
        this.plugin = plugin;
        this.regionManager = regionManager;
        this.effectApplier = new EffectApplier();
    }

    // Separate registration method
    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();

        if (clickedBlock == null) return;

        if (!isHandEmpty(player)) return;

        if (isStair(clickedBlock.getType())) {
            Location location = clickedBlock.getLocation();

            if (regionManager.isInRegion(location)) {
                UUID playerId = player.getUniqueId();

                if (!cooldowns.contains(playerId)) {
                    cooldowns.add(playerId);
                    effectApplier.applyRegeneration(player);

                    // Remove cooldown after 1 second (20 ticks)
                    BukkitScheduler scheduler = plugin.getServer().getScheduler();
                    scheduler.runTaskLater(plugin, () -> {
                        cooldowns.remove(playerId);
                    }, 20);
                }
            }
        }
    }

    private boolean isHandEmpty(Player player) {
        return player.getInventory().getItemInMainHand().getType() == Material.AIR;
    }

    private boolean isStair(Material material) {
        return material.name().endsWith("_STAIRS");
    }
}
