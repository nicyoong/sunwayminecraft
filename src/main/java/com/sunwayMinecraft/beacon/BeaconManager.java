package com.sunwayMinecraft.beacon;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * This class manages beacon-related operations, including color transitions and configuration
 * management. It interacts with the configuration loader and the color transition manager to ensure
 * beacons transition between colors smoothly based on configuration settings.
 *
 * <p>The class provides functionality for: - Initializing beacon color transitions. - Pausing and
 * resuming the color transition process. - Reloading configuration settings for beacon transitions.
 * - Updating the number of ticks per transition and restarting the process with the new value.
 *
 * <p>The class ensures that configuration changes are applied smoothly while keeping the transition
 * process running with correct parameters.
 */
public class BeaconManager {
  private final JavaPlugin plugin;
  private final ColorTransition colorTransitionManager;
  private final ConfigurationLoader configLoader;

  /**
   * Constructor for the BeaconManager class. Initializes the manager with the provided plugin
   * instance and sets up the necessary components for managing beacon color transitions.
   *
   * <p>It loads beacon colors and color cycles from the configuration and initializes the
   * ColorTransitionManager with those values.
   *
   * @param plugin The instance of the plugin that this BeaconManager will interact with.
   */
  public BeaconManager(JavaPlugin plugin) {
    this.plugin = plugin;
    this.configLoader = new BeaconConfigurationLoader(plugin);
    Map<Location, Integer> beaconColors = configLoader.loadBeacons();
    List<Material> colorCycle = configLoader.getColorCycle();
    this.colorTransitionManager = new ColorTransitionManager(plugin, beaconColors, colorCycle);
  }

  /**
   * Initializes the beacon color transition process by retrieving the number of ticks per
   * transition from the configuration and starting the color transition using the
   * ColorTransitionManager.
   *
   * <p>This method is typically called when the plugin starts up or when a fresh initialization of
   * the beacon system is required.
   */
  public void initialize() {
    int ticksPerTransition = configLoader.getTicksPerTransition();
    colorTransitionManager.startTransition(plugin, ticksPerTransition, configLoader.loadBeacons());
  }

  /**
   * Pauses the ongoing color transition process, halting any color changes between beacons. It logs
   * the action of pausing the transition for informational purposes.
   */
  public void pauseColorTransition() {
    colorTransitionManager.pause();
    plugin.getLogger().log(Level.INFO, "Beacon color transitions paused.");
  }

  /**
   * Resumes the paused color transition process, allowing the beacons to continue transitioning
   * between colors. It logs the action of resuming the transition for informational purposes.
   */
  public void resumeColorTransition() {
    colorTransitionManager.resume(plugin);
    plugin.getLogger().log(Level.INFO, "Beacon color transitions resumed.");
  }

  /**
   * Reloads the beacon-related configuration, including the number of ticks per transition and the
   * color cycle. This method can be used when the configuration file has been updated and needs to
   * be reloaded dynamically.
   *
   * <p>Logs an informational message indicating that the configuration has been reloaded, and may
   * reset the ColorTransitionManager with new values if needed.
   */
  public void reloadConfiguration() {
    int ticksPerTransition = configLoader.getTicksPerTransition();
    List<Material> colorCycle = configLoader.getColorCycle();
    // You might want to reset the ColorTransitionManager with new parameters
    plugin.getLogger().log(Level.INFO, "Configuration reloaded.");
  }

  /**
   * Updates the number of ticks per transition for beacon color changes. If the provided value is
   * invalid (less than or equal to zero), it logs a warning and does not apply the new value.
   *
   * <p>The method pauses the current color transition, updates the tick value, and restarts the
   * transition process with the new tick value.
   *
   * @param newTicks The new number of ticks per transition to be applied. Must be greater than 0.
   */
  public void setTicksPerTransition(int newTicks) {
    // Update the ticks per transition
    if (newTicks <= 0) {
      plugin
          .getLogger()
          .log(
              Level.WARNING,
              "Invalid ticks per transition value: " + newTicks + ". Must be greater than 0.");
      return; // Prevent invalid tick values
    }

    // Cancel the existing color transition task
    colorTransitionManager.pause(); // Pause the current task to avoid conflicts

    // Update the ticks per transition
    long previousTicks =
        colorTransitionManager.getTicksPerTransition(); // Retrieve the previous ticks for logging
    plugin
        .getLogger()
        .log(Level.INFO, "Updating ticks per transition from " + previousTicks + " to " + newTicks);

    // Start the color transition with the new ticks value
    colorTransitionManager.startTransition(
        plugin, newTicks, configLoader.loadBeacons()); // Restart with new settings

    // Log the update for confirmation
    plugin.getLogger().log(Level.INFO, "Ticks per transition updated to " + newTicks + " ticks.");
  }
}
