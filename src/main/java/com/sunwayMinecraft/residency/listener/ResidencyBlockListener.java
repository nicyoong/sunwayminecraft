package com.sunwayMinecraft.residency.listener;

import com.sunwayMinecraft.residency.ResidencyManager;
import com.sunwayMinecraft.residency.access.AccessDecision;
import com.sunwayMinecraft.residency.domain.ActionType;
import com.sunwayMinecraft.residency.util.Message;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Openable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class ResidencyBlockListener implements Listener {
    private final ResidencyManager manager;

    public ResidencyBlockListener(ResidencyManager manager) { this.manager = manager; }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!manager.isManagedLocation(event.getBlock().getLocation())) return;
        AccessDecision decision = manager.getAccessService().check(event.getPlayer(), event.getBlock().getLocation(), ActionType.BREAK_BLOCK);
        if (!decision.isAllowed()) { event.setCancelled(true); event.getPlayer().sendMessage(Message.error(decision.getDenialReason())); }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!manager.isManagedLocation(event.getBlock().getLocation())) return;
        AccessDecision decision = manager.getAccessService().check(event.getPlayer(), event.getBlock().getLocation(), ActionType.PLACE_BLOCK);
        if (!decision.isAllowed()) { event.setCancelled(true); event.getPlayer().sendMessage(Message.error(decision.getDenialReason())); }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null || !manager.isManagedLocation(block.getLocation())) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.PHYSICAL) return;
        ActionType type = ActionType.USE_BUTTON_OR_LEVER;
        if (block.getBlockData() instanceof Openable) type = ActionType.OPEN_DOOR;
        else if (isContainer(block.getType())) type = ActionType.USE_CONTAINER;
        AccessDecision decision = manager.getAccessService().check(event.getPlayer(), block.getLocation(), type);
        if (!decision.isAllowed()) { event.setCancelled(true); event.getPlayer().sendMessage(Message.error(decision.getDenialReason())); }
    }

    @EventHandler(ignoreCancelled = true)
    public void onArmorStand(PlayerArmorStandManipulateEvent event) {
        if (!manager.isManagedLocation(event.getRightClicked().getLocation())) return;
        AccessDecision decision = manager.getAccessService().check(event.getPlayer(), event.getRightClicked().getLocation(), ActionType.MODIFY_ARMOR_STAND);
        if (!decision.isAllowed()) { event.setCancelled(true); event.getPlayer().sendMessage(Message.error(decision.getDenialReason())); }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBed(PlayerBedEnterEvent event) {
        if (!manager.isManagedLocation(event.getBed().getLocation())) return;
        AccessDecision decision = manager.getAccessService().check(event.getPlayer(), event.getBed().getLocation(), ActionType.USE_BED);
        if (!decision.isAllowed()) { event.setCancelled(true); event.getPlayer().sendMessage(Message.error(decision.getDenialReason())); }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractAtEntityEvent event) {
        if (!manager.isManagedLocation(event.getRightClicked().getLocation())) return;
        ActionType type = event.getRightClicked().getType() == EntityType.ITEM_FRAME ? ActionType.MODIFY_ITEM_FRAME : ActionType.INTERACT_ENTITY;
        if (event.getRightClicked() instanceof Tameable) type = ActionType.INTERACT_PET;
        AccessDecision decision = manager.getAccessService().check(event.getPlayer(), event.getRightClicked().getLocation(), type);
        if (!decision.isAllowed()) { event.setCancelled(true); event.getPlayer().sendMessage(Message.error(decision.getDenialReason())); }
    }

    private boolean isContainer(Material material) {
        return material.name().contains("CHEST") || material.name().contains("BARREL") || material.name().contains("SHULKER_BOX")
                || material.name().contains("FURNACE") || material.name().contains("HOPPER") || material.name().contains("DISPENSER")
                || material.name().contains("DROPPER") || material.name().contains("BREWING_STAND");
    }
}
