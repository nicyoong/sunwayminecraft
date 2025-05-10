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
}
