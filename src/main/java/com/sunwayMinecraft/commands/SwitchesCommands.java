package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.switches.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The SwitchesCommands class handles commands related to light switches in the SunwayMinecraft
 * plugin. It processes several commands such as scanning lights, exporting light data, listing
 * light regions, providing light block information, checking the player's current light region, and
 * reloading configurations.
 *
 * <p>The class implements the CommandExecutor interface, allowing it to handle various commands
 * that players or admins can execute in-game. The supported commands include:
 *
 * <ul>
 *   <li><code>/scanlights</code>: Scans for light blocks (Sea Lantern, Glowstone, Jack-o-Lantern)
 *       in the current region.
 *   <li><code>/exportlights &lt;filename&gt;</code>: Exports all found light blocks to a CSV file.
 *   <li><code>/listlightregions</code>: Lists all configured light regions.
 *   <li><code>/checklightregion</code>: Checks which light region the player is currently in.
 *   <li><code>/lightinfo</code>: Displays information about the light block the player is looking
 *       at, including associated regions and switches.
 *   <li><code>/reloadsunwayswitches</code>: Reloads light and switch configurations from disk.
 * </ul>
 *
 * <p>Key functionality includes:
 *
 * <ul>
 *   <li>Permission checks to ensure the sender has the required permission for reload.
 *   <li>Scanning regions for light blocks via LightManager.
 *   <li>Exporting block data to files for external processing.
 *   <li>Dynamic listing and checking of configured regions.
 *   <li>Reporting detailed block info and linked button switches.
 * </ul>
 *
 * <p>The main methods provided by this class are:
 *
 * <ul>
 *   <li><code>handleScanLights</code>: Scans and reports lights in the current region.
 *   <li><code>handleExportLights</code>: Exports region lights to a file.
 *   <li><code>handleListRegions</code>: Lists all configured light regions.
 *   <li><code>handleCheckRegion</code>: Checks and notifies the player’s light region.
 *   <li><code>handleLightInfo</code>: Provides detailed info about a targeted light block.
 *   <li><code>handleReload</code>: Reloads configurations and reinitializes managers.
 * </ul>
 */
public class SwitchesCommands implements CommandExecutor {
  private final JavaPlugin plugin;
  private final LightConfigManager lightConfig;
  private final SwitchConfigManager switchConfig;
  private final LightManager lightManager;
  private SwitchManager switchManager;

  /**
   * Constructs a new SwitchesCommands handler.
   *
   * @param plugin the main plugin instance
   * @param lightConfig the manager handling light region configurations
   * @param switchConfig the manager handling switch configurations
   */
  public SwitchesCommands(
      JavaPlugin plugin, LightConfigManager lightConfig, SwitchConfigManager switchConfig) {
    this.plugin = plugin;
    this.lightConfig = lightConfig;
    this.switchConfig = switchConfig;
    this.lightManager = new LightManager(lightConfig);
    this.switchManager = new SwitchManager(switchConfig, lightConfig);
  }

  /**
   * Dispatches incoming commands to their respective handlers based on command name.
   *
   * <p>Supported commands are: scanlights, exportlights, listlightregions, checklightregion,
   * lightinfo, reloadsunwayswitches.
   *
   * @param sender the source of the command (player or console)
   * @param cmd the command that was executed
   * @param label the alias of the command used
   * @param args any arguments provided with the command
   * @return true if the command was handled, false to show usage
   */
  @Override
  public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage("§cThis command can only be used by players!");
      return true;
    }

    try {
      switch (cmd.getName().toLowerCase()) {
        case "scanlights" -> handleScanLights(player);
        case "exportlights" -> handleExportLights(player, args);
        case "listlightregions" -> handleListRegions(player);
        case "checklightregion" -> handleCheckRegion(player);
        case "lightinfo" -> handleLightInfo(player);
        case "reloadsunwayswitches" -> handleReload(sender);
        default -> {
          return false;
        }
      }
    } catch (IllegalArgumentException e) {
      player.sendMessage("§cError: " + e.getMessage());
    } catch (Exception e) {
      player.sendMessage("§cAn unexpected error occurred!");
      plugin.getLogger().severe(e.getMessage());
    }
    return true;
  }

  /**
   * Handles the <code>/reloadsunwayswitches</code> command, reloading configurations for lights and
   * switches.
   *
   * <p>Steps performed:
   *
   * <ol>
   *   <li>Checks if sender has <code>sunwayminecraft.switches.reload</code> permission.
   *   <li>Reloads lightConfig and switchConfig from disk.
   *   <li>Reinitializes the SwitchManager with updated configs.
   *   <li>Sends summary messages including region and switch counts.
   * </ol>
   *
   * @param sender the command sender (player or console)
   * @return always true after attempting reload
   */
  private boolean handleReload(CommandSender sender) {
    if (!sender.hasPermission("sunwayminecraft.switches.reload")) {
      sender.sendMessage("§cNo permission!");
      return true;
    }

    try {
      // Reload configs using instance methods
      lightConfig.reload();
      switchConfig.reload();

      // Reinitialize dependent components with instances
      switchManager = new SwitchManager(switchConfig, lightConfig);

      sender.sendMessage("§aReloaded configurations:");
      sender.sendMessage("§a- Light regions: " + lightConfig.getRegions().size());
      sender.sendMessage("§a- Button switches: " + switchConfig.getSwitches().size());
      return true;
    } catch (Exception e) {
      sender.sendMessage("§cReload failed: " + e.getMessage());
      return true;
    }
  }

  /**
   * Handles the <code>/scanlights</code> command, which scans the current light region for blocks.
   *
   * <p>Steps performed:
   *
   * <ol>
   *   <li>Retrieves the player's current region via <code>getCurrentRegion</code>.
   *   <li>Uses <code>lightManager.scanRegion</code> to find all supported light blocks.
   *   <li>Sends the count and formatted locations of each found block to the player.
   * </ol>
   *
   * @param player the player initiating the scan
   */
  private void handleScanLights(Player player) {
    LightRegion region = getCurrentRegion(player);
    List<Block> lights = lightManager.scanRegion(region, player);

    player.sendMessage("§6Found " + lights.size() + " light blocks:");
    lights.forEach(b -> player.sendMessage(formatBlockLocation(b)));
  }

  /**
   * Handles the <code>/exportlights &lt;filename&gt;</code> command, exporting region light blocks
   * to a CSV file.
   *
   * <p>Steps performed:
   *
   * <ol>
   *   <li>Validates presence of filename argument.
   *   <li>Scans current region for light blocks.
   *   <li>Writes each block's material and coordinates to the specified file.
   *   <li>Notifies the player of success or throws on I/O failure.
   * </ol>
   *
   * @param player the player initiating the export
   * @param args command arguments, where args[0] is the target filename
   * @throws IllegalArgumentException if filename is missing or file cannot be written
   */
  private void handleExportLights(Player player, String[] args) {
    if (args.length < 1) throw new IllegalArgumentException("§cUsage: /exportlights <filename>");

    LightRegion region = getCurrentRegion(player);
    List<Block> lights = lightManager.scanRegion(region, player);
    File outputFile = new File(plugin.getDataFolder(), args[0]);

    try (FileWriter writer = new FileWriter(outputFile)) {
      for (Block block : lights) {
        writer.write(
            block.getType() + "," + block.getX() + "," + block.getY() + "," + block.getZ() + "\n");
      }
      player.sendMessage(
          "§aSuccessfully exported " + lights.size() + " lights to " + outputFile.getName());
    } catch (IOException e) {
      throw new IllegalArgumentException("§cFailed to write file: " + e.getMessage());
    }
  }

  /**
   * Handles the <code>/listlightregions</code> command, listing all defined light regions.
   *
   * <p>Steps performed:
   *
   * <ol>
   *   <li>Retrieves region map from <code>lightConfig</code>.
   *   <li>If empty, notifies that no regions are defined.
   *   <li>Otherwise, lists each region name to the player.
   * </ol>
   *
   * @param player the player requesting the list
   */
  private void handleListRegions(Player player) {
    Map<String, LightRegion> regions = lightConfig.getRegions();
    if (regions.isEmpty()) {
      player.sendMessage("§eNo light regions defined.");
      return;
    }

    player.sendMessage("§6Defined light regions:");
    regions.forEach(
        (name, region) -> {
          if (name != null) {
            player.sendMessage("§a- " + name);
          }
        });
  }

  /**
   * Handles the <code>/checklightregion</code> command, checking if the player is in a light
   * region.
   *
   * <p>Steps performed:
   *
   * <ol>
   *   <li>Gets player location.
   *   <li>Finds any region containing that location.
   *   <li>Notifies player of the region name or absence thereof.
   * </ol>
   *
   * @param player the player to check
   */
  private void handleCheckRegion(Player player) {
    Location loc = player.getLocation();
    Optional<LightRegion> region =
        lightConfig.getRegions().values().stream().filter(r -> r.contains(loc)).findFirst();

    if (region.isPresent()) {
      player.sendMessage("§aYou're in light region: §6" + region.get().name());
    } else {
      player.sendMessage("§eYou're not in any light region.");
    }
  }

  /**
   * Handles the <code>/checklightregion</code> command, checking if the player is in a light
   * region.
   *
   * <p>Steps performed:
   *
   * <ol>
   *   <li>Gets player location.
   *   <li>Finds any region containing that location.
   *   <li>Notifies player of the region name or absence thereof.
   * </ol>
   *
   * @param player the player to check
   */
  private void handleLightInfo(Player player) {
    Block target = player.getTargetBlockExact(5);
    if (target == null || !LightManager.isLightBlock(target.getType())) {
      throw new IllegalArgumentException(
          "§cLook at a light block (Sea Lantern, Glowstone, or Jack-o-Lantern)");
    }

    Location blockLoc = target.getLocation();
    World currentWorld = player.getWorld();

    List<LightRegion> regions =
        lightConfig.getRegions().values().stream()
            .filter(r -> r.world().getName().equals(currentWorld.getName()))
            .filter(r -> r.contains(blockLoc))
            .collect(Collectors.toList());

    // Get switches with precise location comparison
    List<ButtonSwitch> switches =
        switchConfig.getSwitches().values().stream()
            .filter(
                s ->
                    s.lightLocations().stream()
                        .anyMatch(
                            loc ->
                                loc != null
                                    && loc.getWorld() != null
                                    && loc.getWorld().getName().equals(currentWorld.getName())
                                    && loc.getBlockX() == blockLoc.getBlockX()
                                    && loc.getBlockY() == blockLoc.getBlockY()
                                    && loc.getBlockZ() == blockLoc.getBlockZ()))
            .collect(Collectors.toList());
    player.sendMessage("§6=== Light Block Info ===");
    player.sendMessage(formatBlockLocation(target));

    if (!regions.isEmpty()) {
      player.sendMessage("§aBelongs to regions:");
      regions.forEach(r -> player.sendMessage("§b- " + r.name()));
    } else {
      player.sendMessage("§cNot part of any light region!");
    }

    if (!switches.isEmpty()) {
      player.sendMessage("§aControlled by buttons:");
      switches.forEach(s -> player.sendMessage("§b- " + formatLocation(s.buttonLocation())));
    } else {
      player.sendMessage("§cNot linked to any switches!");
    }
  }

  /**
   * Retrieves the unique light region containing the player.
   *
   * <p>Steps performed:
   *
   * <ol>
   *   <li>Filters configured regions by the player's location.
   *   <li>Throws if none or multiple regions match.
   * </ol>
   *
   * @param player the player whose location is checked
   * @return the single LightRegion containing the player
   * @throws IllegalArgumentException if zero or multiple regions are found
   */
  private LightRegion getCurrentRegion(Player player) {
    Location loc = player.getLocation();
    List<LightRegion> regions =
        lightConfig.getRegions().values().stream()
            .filter(r -> r.contains(loc))
            .collect(Collectors.toList());

    if (regions.isEmpty()) {
      throw new IllegalArgumentException("§cYou're not in any light region!");
    }
    if (regions.size() > 1) {
      throw new IllegalArgumentException(
          "§cYou're in multiple regions! Please move to a unique region.");
    }
    return regions.get(0);
  }

  /**
   * Formats a block's type and coordinates into a player-friendly string.
   *
   * @param block the block to format
   * @return formatted string containing material and position
   */
  private String formatBlockLocation(Block block) {
    return "§b"
        + String.format(
            "%s @ [%d, %d, %d]",
            formatMaterial(block.getType()), block.getX(), block.getY(), block.getZ());
  }

  /**
   * Formats a Location object into a descriptive string.
   *
   * @param loc the location to format
   * @return string describing the world and coordinates
   */
  private String formatLocation(Location loc) {
    return String.format(
        "World: %s, X: %d, Y: %d, Z: %d",
        loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
  }

  /**
   * Converts a Material enum into a human-readable lowercase string.
   *
   * @param material the material to format
   * @return formatted material name with spaces instead of underscores
   */
  private String formatMaterial(Material material) {
    return "§e" + material.toString().replace("_", " ").toLowerCase();
  }
}
