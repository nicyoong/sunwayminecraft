package com.sunwayMinecraft.switches;

import com.sunwayMinecraft.regions.RegionManager;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.Lightable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Map;

public class SwitchListener implements Listener {

  private final RegionManager regionManager;
  private final SwitchConfigManager switchConfigManager;

  public SwitchListener(RegionManager regionManager, SwitchConfigManager switchConfigManager) {
    this.regionManager = regionManager;
    this.switchConfigManager = switchConfigManager;
  }

  @EventHandler
  public void onButtonPress(PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

    Block block = event.getClickedBlock();
    if (block == null) return;

    // Check if it's a button
    String material = block.getType().name();
    if (!material.endsWith("_BUTTON")) return;

    Location buttonLoc = block.getLocation();
    Map<Location, ButtonSwitch> switches = switchConfigManager.getSwitches();

    ButtonSwitch buttonSwitch = switches.get(buttonLoc);
    if (buttonSwitch == null) return;

    event.setCancelled(true); // Prevent accidental use

    Player player = event.getPlayer();
    boolean hasAnyPermission = false;
    // Toggle lights using region-based permissions
    for (Location lightLoc : buttonSwitch.lightLocations()) {
      if (regionManager.canModifyAtLocation(event.getPlayer(), lightLoc)) {
        hasAnyPermission = true;
        toggleLight(lightLoc.getBlock());
      }
    }
    if (!hasAnyPermission) {
      player.sendMessage("§cYou don't have permission to control these lights!");
    }
  }

  private void toggleLight(Block block) {
    if (block.getBlockData() instanceof Lightable) {
      Lightable lightable = (Lightable) block.getBlockData();
      lightable.setLit(!lightable.isLit());
      block.setBlockData(lightable);
    }
  }
}