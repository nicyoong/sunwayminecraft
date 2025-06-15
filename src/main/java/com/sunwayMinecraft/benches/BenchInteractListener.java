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

/**
 * This class listens for player interactions with benches in the Minecraft world. It handles the
 * interaction event when a player right-clicks on a bench block (stairs) and applies effects such
 * as regeneration, provided that the player is not on cooldown.
 *
 * <p>The BenchInteractListener listens for the `PlayerInteractEvent` when the player right-clicks a
 * block. If the block is a valid stair and the player is not holding an item in their main hand,
 * the listener will apply certain effects to the player. A cooldown is enforced to prevent players
 * from repeatedly triggering the effects too quickly.
 *
 * <p>Key functionality includes: - Checking if the player is interacting with a valid bench
 * (stairs) in the world. - Applying the regeneration effect to the player. - Enforcing a cooldown
 * to prevent multiple uses in quick succession. - Ensuring that only players with an empty hand can
 * interact with the bench.
 *
 * <p>The listener also manages a cooldown system using a `Map` of player UUIDs and cooldown end
 * times. The cooldown is set in ticks (80 ticks) and is tracked in milliseconds.
 *
 * <p>The main methods provided by this class are: - `onPlayerInteract()`: Handles the right-click
 * event when the player interacts with a bench. - `isHandEmpty()`: Checks if the player's main hand
 * is empty (they are not holding any item). - `isValidStair()`: Checks if the block the player
 * interacts with is a valid stair block (bottom half). - `register()`: Registers this listener with
 * the plugin's event system.
 */
public class BenchInteractListener implements Listener {
  private final SunwayMinecraft plugin;
  private final RegionManager regionManager;
  private final EffectApplier effectApplier;
  private final Map<UUID, Long> cooldownEndTimes = new HashMap<>(); // Changed to Map
  private static final int COOLDOWNTICKS = 80;

  /**
   * Constructs a new BenchInteractListener instance with the provided SunwayMinecraft plugin and
   * RegionManager.
   *
   * <p>This constructor initializes the listener by setting the plugin and region manager
   * instances, which are necessary for handling interactions with benches and checking whether the
   * player is within a specific region. It also creates an instance of `EffectApplier`, which is
   * used to apply effects to players interacting with the bench.
   *
   * @param plugin The SunwayMinecraft plugin instance, used to interact with the plugin's server
   *     and event system.
   * @param regionManager The RegionManager instance, used to check if the player is within a
   *     specific region where bench interactions are allowed.
   */
  public BenchInteractListener(SunwayMinecraft plugin, RegionManager regionManager) {
    this.plugin = plugin;
    this.regionManager = regionManager;
    this.effectApplier = new EffectApplier();
  }

  // Separate registration method
  public void register() {
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
  }

  /**
   * This method handles the `PlayerInteractEvent` when a player right-clicks a block. It checks
   * whether the clicked block is a valid stair (bench) and applies regeneration effects to the
   * player if they are not on cooldown.
   *
   * <p>The method follows these steps: 1. Verifies that the action is a right-click on a block.
   *
   * <p>2. Checks if the clicked block is not null and if the player is holding an empty hand.
   *
   * <p>3. Validates if the clicked block is a valid stair and if the player clicked the correct
   * side (top of the stair).
   *
   * <p>4. If the player is within an allowed region (determined by the `RegionManager`), it checks
   * whether the player is currently on cooldown.
   *
   * <p>5. If the player is on cooldown, the remaining time is displayed in seconds, and the effect
   * is not applied.
   *
   * <p>6. If the cooldown has expired or is not present, the regeneration effect is applied to the
   * player.
   *
   * <p>7. A cooldown is applied by storing the player's UUID and the cooldown end time in the
   * `cooldownEndTimes` map.
   *
   * <p>8. The cooldown end time is scheduled to be removed after a set number of ticks (80 ticks,
   * equivalent to 4 seconds).
   *
   * <p>The player will not be able to interact with the bench again until the cooldown expires.
   *
   * @param event The PlayerInteractEvent triggered when the player right-clicks a block.
   */
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
      long cooldownEnd = currentTime + (COOLDOWNTICKS * 50); // Convert ticks to milliseconds
      cooldownEndTimes.put(playerId, cooldownEnd);

      // Schedule cooldown removal
      plugin
          .getServer()
          .getScheduler()
          .runTaskLater(
              plugin,
              () -> {
                cooldownEndTimes.remove(playerId);
              },
              COOLDOWNTICKS);
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
}
