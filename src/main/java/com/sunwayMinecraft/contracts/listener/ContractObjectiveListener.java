package com.sunwayMinecraft.contracts.listener;

import com.sunwayMinecraft.contracts.service.ContractObjectiveService;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Event.Result;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

/** Bridges Bukkit player actions to the contract objective service. */
public class ContractObjectiveListener implements Listener {
    private final ContractObjectiveService objectiveService;

    public ContractObjectiveListener(ContractObjectiveService objectiveService) {
        this.objectiveService = objectiveService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.useInteractedBlock() == Result.DENY || event.getAction() != Action.RIGHT_CLICK_BLOCK
                || event.getClickedBlock() == null) return;
        objectiveService.recordInteraction(event.getPlayer(), event.getClickedBlock().getLocation());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        // cheap early-out: only care when the block coordinate actually changed
        if (to == null || (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ())) {
            return;
        }
        objectiveService.recordMovement(event.getPlayer(), to);
    }
}
