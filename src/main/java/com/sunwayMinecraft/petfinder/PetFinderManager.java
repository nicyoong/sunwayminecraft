package com.sunwayMinecraft.petfinder;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Cat;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.entity.*;

import java.util.*;

public class PetFinderManager {
    private final JavaPlugin plugin;
    private boolean isSearchRunning = false;

    public PetFinderManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void startSearch(CommandSender sender, UUID targetUUID, BoundingBox area) {
        if (isSearchRunning) {
            sender.sendMessage("§cA search is already in progress.");
            return;
        }

        isSearchRunning = true;
        sender.sendMessage("§aStarting pet search..." + (area != null ? " In specified area." : ""));

        List<Entity> entitiesToCheck = new ArrayList<Entity>();
        for (World world : Bukkit.getWorlds()) {
            addEntitiesByType(world, Wolf.class, entitiesToCheck, area);
            addEntitiesByType(world, Cat.class, entitiesToCheck, area);
        }

        new PetSearchTask(plugin, sender, new ArrayList<>(entitiesToCheck), targetUUID, area, this);
    }
    public void setSearchComplete() {
        isSearchRunning = false;
    }

    private <T extends Entity> void addEntitiesByType(World world, Class<T> entityClass,
                                                      List<Entity> list, BoundingBox area) {
        for (T entity : world.getEntitiesByClass(entityClass)) {
            if (area == null || area.contains(entity.getLocation().toVector())) {
                list.add(entity);
            }
        }
    }
    }
