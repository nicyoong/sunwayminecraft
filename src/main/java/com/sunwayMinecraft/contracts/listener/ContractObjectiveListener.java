package com.sunwayMinecraft.contracts.listener;

import com.sunwayMinecraft.contracts.service.ContractObjectiveService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Event.Result;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

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
}
