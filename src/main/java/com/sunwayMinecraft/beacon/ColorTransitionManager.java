package com.sunwayMinecraft.beacon;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Beacon;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * This class manages the color transition process for beacons. It is responsible for controlling
 * the color cycle, starting, pausing, and resuming the beacon color transition, as well as updating
 * the beacon colors based on a predefined cycle.
 *
 * <p>The ColorTransitionManager uses a map of beacon locations and their associated colors, as well
 * as a list of materials representing the color cycle. It handles transitions of beacon colors over
 * time, updating the blocks to show the appropriate colors at each stage.
 *
 * <p>The main methods provided by this class are: - `startTransition()`: Starts the color
 * transition process. - `pause()`: Pauses the color transition process. - `resume()`: Resumes the
 * color transition process. - `getTicksPerTransition()`: Retrieves the number of ticks per
 * transition.
 */
public class ColorTransitionManager implements ColorTransition {
  private final JavaPlugin plugin;
  private Map<Location, Integer> beaconColors;
  private List<Material> colorCycle;
  private int currentColorIndex = 0;
  private int currentBinaryCycle = 0;
  private int ticksPerTransition;
  private BukkitTask colorTransitionTask;

  /**
   * Constructor for the ColorTransitionManager class. Initializes the manager with the provided
   * plugin instance, beacon color map, and color cycle list. This constructor sets up the necessary
   * components to manage beacon color transitions.
   *
   * @param plugin The instance of the plugin that this ColorTransitionManager will interact with.
   * @param beaconColors A map of beacon locations and their respective color indices.
   * @param colorCycle A list of materials representing the color cycle for the beacons.
   */
  public ColorTransitionManager(
      JavaPlugin plugin, Map<Location, Integer> beaconColors, List<Material> colorCycle) {
    this.plugin = plugin;
    this.beaconColors = beaconColors;
    this.colorCycle = colorCycle;
  }

  /**
   * Starts the color transition process for the beacons. It updates the color of the beacons based
   * on the provided color cycle, using a binary cycle to control the transition between colors over
   * time.
   *
   * <p>The method schedules a task that runs periodically, updating the colors of the beacons
   * according to the specified ticks per transition. If a transition is already running, it cancels
   * the existing task before starting a new one.
   *
   * @param plugin The instance of the plugin that this transition will be associated with.
   * @param ticksPerTransition The number of ticks between each color change in the transition
   *     process.
   * @param beaconColors A map of beacon locations and their color indices, used to update the
   *     colors of the beacons.
   */
  @Override
  public void startTransition(
      JavaPlugin plugin, int ticksPerTransition, Map<Location, Integer> beaconColors) {
    this.ticksPerTransition = ticksPerTransition;
    this.beaconColors = beaconColors; // Update beacon colors if needed

    // Ensure any running task is canceled before starting a new one
    if (colorTransitionTask != null && !colorTransitionTask.isCancelled()) {
      colorTransitionTask.cancel();
    }

    // Schedule a new task
    colorTransitionTask =
        new BukkitRunnable() {
          @Override
          public void run() {
            if (ColorTransitionManager.this.ticksPerTransition <= 0) return;

            for (Location loc : beaconColors.keySet()) {
              updateBeaconColor(loc, currentBinaryCycle);
            }

            currentBinaryCycle++;

            if (currentBinaryCycle >= 16) { // 1 << 4 = 16
              currentBinaryCycle = 0;
              currentColorIndex = (currentColorIndex + 1) % colorCycle.size();
            }
          }
        }.runTaskTimer(plugin, 0, this.ticksPerTransition);
    plugin
        .getLogger()
        .log(
            Level.INFO,
            "Started beacon color transitions with "
                + ticksPerTransition
                + " ticks per transition.");
  }

  /**
   * Pauses the ongoing beacon color transition. It cancels the current task if it is running and
   * logs the action. This method is used to halt the transition process temporarily.
   */
  @Override
  public void pause() {
    if (colorTransitionTask != null && !colorTransitionTask.isCancelled()) {
      colorTransitionTask.cancel(); // Cancel the scheduled task
      plugin.getLogger().log(Level.INFO, "Beacon color transitions paused.");
    }
  }

  /**
   * Resumes the paused color transition process. If no transition is currently running or if the
   * existing task was cancelled, it starts a new transition process. If a transition is already
   * running, it logs a warning and does not attempt to resume.
   *
   * @param plugin The instance of the plugin that the transition is associated with.
   */
  @Override
  public void resume(JavaPlugin plugin) {
    if (colorTransitionTask == null || colorTransitionTask.isCancelled()) {
      startTransition(plugin, this.ticksPerTransition, beaconColors); // Start the task anew
      plugin.getLogger().log(Level.INFO, "Beacon color transitions resumed.");
    } else {
      plugin.getLogger().warning("Transition is already running. You cannot resume.");
    }
  }

  /**
   * Retrieves the number of ticks per transition, which controls the speed of the beacon color
   * changes. This value determines the frequency at which the beacon colors update during the
   * transition process.
   *
   * @return The number of ticks per transition.
   */
  @Override
  public long getTicksPerTransition() {
    return ticksPerTransition;
  }

  /**
   * Updates the color of a beacon at a specified location based on the current cycle index and
   * binary cycle. The method updates the colors of the beacon's surrounding glass blocks, switching
   * between colors based on the binary sequence for each position.
   *
   * <p>The beacon's colors are determined by the color cycle, and the binary sequence is used to
   * create a visually appealing transition between colors.
   *
   * @param location The location of the beacon to update.
   * @param binaryCycleIndex The index of the binary cycle used to determine the transition pattern.
   */
  private void updateBeaconColor(Location location, int binaryCycleIndex) {
    int[] binarySequence = {
      0, 16, 24, 20, 28, 18, 26, 22,
      30, 17, 25, 21, 29, 19, 27, 23
    };

    for (int i = 0; i < 5; i++) {
      Location glassBlockLocation = location.clone().add(0, 5 - i, 0);
      Block glassBlock = glassBlockLocation.getBlock();

      Material newColor = colorCycle.get(currentColorIndex);
      Material oldColor =
          colorCycle.get((currentColorIndex + colorCycle.size() - 1) % colorCycle.size());

      int colorPosition = binarySequence[binaryCycleIndex % binarySequence.length];
      int bitState = (colorPosition & (1 << i)) != 0 ? 1 : 0;

      glassBlock.setType(bitState == 1 ? newColor : oldColor);
    }

    Block block = location.getBlock();
    if (block.getType() == Material.BEACON) {
      Beacon beacon = (Beacon) block.getState();
      beacon.update();
    }
  }
}
