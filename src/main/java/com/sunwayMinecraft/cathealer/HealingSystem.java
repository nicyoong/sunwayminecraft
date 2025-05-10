package com.sunwayMinecraft.cathealer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.attribute.Attribute;
import java.util.Collection;

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
        return (int) owner.getWorld().getNearbyEntities(ownerLocation, 10, 10, 10)
                .stream()
                .filter(Cat.class::isInstance)
                .map(Cat.class::cast)
                .filter(cat -> isValidHealingCat(cat, owner))
                .count();
    }

    private boolean isValidHealingCat(Cat cat, Player owner) {
        return cat.isTamed() &&
                owner.equals(cat.getOwner()) &&
                !cat.isSitting() &&
                isWithinRadius(cat.getLocation(), owner.getLocation(), 10);
    }

    private boolean isWithinRadius(Location a, Location b, double radius) {
        return a.distanceSquared(b) <= (radius * radius);
    }
}
