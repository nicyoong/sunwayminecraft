package com.sunwayMinecraft.switches;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * The CelestialLightScheduler class extends {@link org.bukkit.scheduler.BukkitRunnable} to
 * automatically toggle configured light switches at Minecraft “midnight” and “dawn” based on the
 * in-game world time.
 *
 * <p>This scheduler:
 *
 * <ul>
 *   <li>Polls the specified world’s time on each run.
 *   <li>Turns all lights off at midnight (tick 18000 ±20).
 *   <li>Turns all lights on at dawn (tick 0 ±1, i.e. 23959–23999 rolling over).
 *   <li>Prevents duplicate toggles by tracking the last processed time.
 * </ul>
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * CelestialLightScheduler scheduler =
 *     new CelestialLightScheduler(switchConfigManager, "world");
 * scheduler.runTaskTimer(plugin, 0L, 20L); // schedule to run every second
 * }</pre>
 */
public class CelestialLightScheduler extends BukkitRunnable {
  private final SwitchConfigManager switchConfig;
  private final String worldName;
  private int lastProcessedTime = -1;

  public CelestialLightScheduler(SwitchConfigManager switchConfig, String worldName) {
    this.switchConfig = switchConfig;
    this.worldName = worldName;
  }

  /**
   * Called periodically by the Bukkit scheduler. Checks the configured world’s current time and
   * toggles all lights off at midnight (around tick 18000) or on at dawn (around tick 0), ensuring
   * each toggle occurs only once per cycle.
   *
   * <p>The method performs the following steps:
   *
   * <ol>
   *   <li>Obtains the {@link org.bukkit.World} by {@code worldName}; returns immediately if null.
   *   <li>Retrieves the current world time in ticks.
   *   <li>If time is between 17980 and 18020 (midnight) and not yet processed at 18000, invokes
   *       {@link #toggleAllLights(boolean)} with {@code false} to turn lights off, then updates
   *       {@code lastProcessedTime} to 18000.
   *   <li>Else if time is between 23959 and 23999 (dawn) and not yet processed at 0, invokes {@link
   *       #toggleAllLights(boolean)} with {@code true} to turn lights on, then updates {@code
   *       lastProcessedTime} to 0.
   *   <li>Else if the current tick is outside both ranges, resets {@code lastProcessedTime} to –1
   *       to allow the next cycle of toggles.
   * </ol>
   */
  @Override
  public void run() {
    World world = Bukkit.getWorld("world"); // Use your world name
    if (world == null) return;

    long currentTime = world.getTime();

    // Handle midnight (turn off)
    if (isBetween(currentTime, 17980, 18020) && lastProcessedTime != 18000) {
      toggleAllLights(false);
      lastProcessedTime = 18000;
    }
    // Toggle on at 0 ± 1 tick (dawn)
    else if (isBetween(currentTime, 23959, 23999) && lastProcessedTime != 0) {
      toggleAllLights(true);
      lastProcessedTime = 0;
    }
    // Reset tracking if outside both ranges
    else if (!isBetween(currentTime, 17980, 18020) && !isBetween(currentTime, 23959, 23999)) {
      lastProcessedTime = -1;
    }
  }

  private boolean isBetween(long value, long min, long max) {
    return (value >= min && value <= max) || (min > max && (value >= min || value <= max));
  }

  /**
   * Toggles all configured light switches on or off.
   *
   * <p>The method performs the following steps:
   *
   * <ol>
   *   <li>Iterates over each {@link ButtonSwitch} in {@code switchConfig}.
   *   <li>For each switch, iterates over its light locations.
   *   <li>Skips any location whose world is null or whose chunk is not loaded.
   *   <li>Determines the target {@link org.bukkit.Material} via {@link #getTargetMaterial(Material,
   *       boolean)} based on {@code turnOn}.
   *   <li>If a valid target material is returned, sets the block at the location to that material.
   * </ol>
   *
   * @param turnOn {@code true} to switch lights on (dawn restore), {@code false} to switch lights
   *     off (midnight)
   */
  private void toggleAllLights(boolean turnOn) {
    switchConfig
        .getSwitches()
        .values()
        .forEach(
            buttonSwitch -> {
              buttonSwitch
                  .lightLocations()
                  .forEach(
                      loc -> {
                        World locWorld = loc.getWorld();
                        if (locWorld == null
                            || !locWorld.isChunkLoaded(
                                loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                          return;
                        }

                        Block block = loc.getBlock();
                        Material targetMaterial = getTargetMaterial(block.getType(), turnOn);

                        if (targetMaterial != null) {
                          block.setType(targetMaterial);
                        }
                      });
            });
  }

  private Material getTargetMaterial(Material current, boolean turnOn) {
    return turnOn
        ? LightManager.getOriginalMaterial(current)
        : // Dawn: restore original
        LightManager.getOffMaterial(current); // Midnight: turn off
  }
}
