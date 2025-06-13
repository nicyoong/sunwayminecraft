package com.sunwayMinecraft.switches;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * The SwitchManager class ties together button switch configurations and light region
 * configurations to handle toggling of light blocks when a switch is activated.
 *
 * <p>This manager is responsible for:
 *
 * <ul>
 *   <li>Retrieving the configured light locations for a given {@link ButtonSwitch}.
 *   <li>Determining the current material at each location and computing its toggle target (light ↔
 *       non-light) using {@link LightManager}.
 *   <li>Applying the material change to each block in the switch’s lightLocations.
 * </ul>
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * SwitchManager switchMgr = new SwitchManager(switchConfigManager, lightConfigManager);
 * ButtonSwitch button = switchConfigManager.getSwitches().get(clickedLocation);
 * switchMgr.toggleLights(button, player);
 * }</pre>
 */
public class SwitchManager {
  private final SwitchConfigManager switchConfig;
  private final LightConfigManager lightConfig;

  public SwitchManager(SwitchConfigManager switchConfig, LightConfigManager lightConfig) {
    this.switchConfig = switchConfig;
    this.lightConfig = lightConfig;
  }

  public void toggleLights(ButtonSwitch buttonSwitch, Player player) {
    buttonSwitch
        .lightLocations()
        .forEach(
            loc -> {
              Block block = loc.getBlock();
              Material current = block.getType();
              if (LightManager.isCopperBulb(current)) {
                LightManager.toggleCopperBulb(block);
                return;
              }
              Material target =
                  LightManager.isLightBlock(current)
                      ? LightManager.getOffMaterial(current)
                      : LightManager.getOriginalMaterial(current);

              if (target != null) {
                block.setType(target);
              }
            });
  }
}
