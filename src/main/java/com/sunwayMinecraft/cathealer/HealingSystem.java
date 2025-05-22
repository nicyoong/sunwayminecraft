package com.sunwayMinecraft.cathealer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.attribute.Attribute;
import java.util.Collection;

/**
 * This class schedules and manages automatic healing for players based on the number of nearby
 * tamed cats they own in a Minecraft world. It retrieves all online players every 2.5 seconds (50
 * ticks) and applies healing of 1 health point per valid cat within a 10-block radius, up to the
 * player's maximum health.
 *
 * <p>Key behaviors include:
 *
 * <ul>
 *   <li><b>Timed Healing Task:</b> A repeating BukkitRunnable that triggers healing checks.
 *   <li><b>Player Scanning:</b> Collects all online players each interval for processing.
 *   <li><b>Cat Validation:</b> Identifies tamed, owned, non-sitting cats within proximity.
 *   <li><b>Health Application:</b> Increases player health based on the count of valid cats.
 * </ul>
 *
 * <p>Methods provided:
 *
 * <ul>
 *   <li><b>HealingSystem(JavaPlugin plugin):</b> Constructor that stores the plugin reference for
 *       task scheduling.
 *   <li><b>start():</b> Begins the repeating healing task with an initial delay of 0 ticks and a
 *       period of 50 ticks.
 *   <li><b>processAllPlayers():</b> Iterates over and processes each online player.
 *   <li><b>processPlayer(Player):</b> Counts active healing cats and applies healing if applicable.
 *   <li><b>countActiveCats(Player):</b> Returns the number of valid healing cats near the player.
 *   <li><b>isValidHealingCat(Cat, Player):</b> Checks taming, ownership, posture, and distance
 *       criteria.
 *   <li><b>isWithinRadius(Location, Location, double):</b> Performs a squared-distance check for
 *       performance.
 *   <li><b>applyCatHealing(Player, int):</b> Calculates and sets the new health value without
 *       exceeding max health.
 * </ul>
 *
 * @param plugin the JavaPlugin instance used to schedule and manage the healing task
 */
public class HealingSystem {
  private final JavaPlugin plugin;

  public HealingSystem(JavaPlugin plugin) {
    this.plugin = plugin;
  }

  public void start() {
    new BukkitRunnable() {
      @Override
      public void run() {
        processAllPlayers();
      }
    }.runTaskTimer(plugin, 0L, 50L); // 50 ticks = 2.5 seconds
  }

  private void processAllPlayers() {
    Collection<? extends Player> players = Bukkit.getOnlinePlayers();
    players.forEach(this::processPlayer);
  }

  private void processPlayer(Player player) {
    int activeCats = countActiveCats(player);
    if (activeCats > 0) {
      applyCatHealing(player, activeCats);
    }
  }

  private int countActiveCats(Player owner) {
    Location ownerLocation = owner.getLocation();
    return (int)
        owner.getWorld().getNearbyEntities(ownerLocation, 10, 10, 10).stream()
            .filter(Cat.class::isInstance)
            .map(Cat.class::cast)
            .filter(cat -> isValidHealingCat(cat, owner))
            .count();
  }

  private boolean isValidHealingCat(Cat cat, Player owner) {
    return cat.isTamed()
        && owner.equals(cat.getOwner())
        && !cat.isSitting()
        && isWithinRadius(cat.getLocation(), owner.getLocation(), 10);
  }

  private boolean isWithinRadius(Location a, Location b, double radius) {
    return a.distanceSquared(b) <= (radius * radius);
  }

  private void applyCatHealing(Player player, int catCount) {
    double maxHealth =
        player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
    double newHealth = Math.min(player.getHealth() + catCount, maxHealth);
    player.setHealth(newHealth);
  }
}
