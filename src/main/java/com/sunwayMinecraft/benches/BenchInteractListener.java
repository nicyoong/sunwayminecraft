package com.sunwayMinecraft.benches;

import com.sunwayMinecraft.SunwayMinecraft;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.*;

public class BenchInteractListener implements Listener {
    private final SunwayMinecraft plugin;
    private final RegionManager regionManager;
    private final EffectApplier effectApplier;
    private final Map<UUID, Long> cooldownEndTimes = new HashMap<>(); // Changed to Map
    private final int COOLDOWN_TICKS = 80;

    public BenchInteractListener(SunwayMinecraft plugin, RegionManager regionManager) {
        this.plugin = plugin;
        this.regionManager = regionManager;
        this.effectApplier = new EffectApplier();
    }

    // Separate registration method
    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();
        BlockFace clickedFace = event.getBlockFace();

        if (clickedBlock == null) return;

        if (!isHandEmpty(player)) return;
        if (!isValidStair(clickedBlock, clickedFace)) return;

        Location location = clickedBlock.getLocation();

        if (regionManager.isInRegion(location)) {
            UUID playerId = player.getUniqueId();
            long currentTime = System.currentTimeMillis();

            // Check if in cooldown
            if (cooldownEndTimes.containsKey(playerId)) {
                long remainingMillis = cooldownEndTimes.get(playerId) - currentTime;

                if (remainingMillis > 0) {
                    // Calculate ceiling seconds
                    int secondsLeft = (int) Math.ceil(remainingMillis / 1000.0);
                    player.sendMessage("§eBench is on cooldown! Wait " + secondsLeft + " more seconds.");
                    return;
                }
            }

            // Apply effects and cooldown
            effectApplier.applyRegeneration(player);
            long cooldownEnd = currentTime + (COOLDOWN_TICKS * 50); // Convert ticks to milliseconds
            cooldownEndTimes.put(playerId, cooldownEnd);

            // Schedule cooldown removal
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                cooldownEndTimes.remove(playerId);
            }, COOLDOWN_TICKS);
        }
    }

    private boolean isHandEmpty(Player player) {
        return player.getInventory().getItemInMainHand().getType() == Material.AIR;
    }

    private boolean isValidStair(Block block, BlockFace clickedFace) {
        // Check if block is a stair
        if (!block.getType().name().endsWith("_STAIRS")) return false;

        // Get block state
        if (!(block.getBlockData() instanceof Stairs stair)) return false;

        // Check orientation (only allow bottom half stairs)
        if (stair.getHalf() != Bisected.Half.BOTTOM) return false;

        // Check click location (must be top face)
        return clickedFace == BlockFace.UP;
    }

    private boolean isStair(Material material) {
        return material.name().endsWith("_STAIRS");
    }
}