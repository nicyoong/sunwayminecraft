package com.sunwayMinecraft.petfinder;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;

import java.util.*;

public class PetFinderManager {
    private final Main plugin;
    private boolean isSearchRunning = false;

    public PetFinderFeature(Main plugin) {
        this.plugin = plugin;
    }

    public void startSearch(CommandSender sender, UUID targetUUID, BoundingBox area) {
        if (isSearchRunning) {
            sender.sendMessage("§cA search is already in progress.");
            return;
        }

        isSearchRunning = true;
        sender.sendMessage("§aStarting pet search..." + (area != null ? " In specified area." : ""));

        List<Entity> entitiesToCheck = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            addEntitiesByType(world, Wolf.class, entitiesToCheck, area);
            addEntitiesByType(world, Cat.class, entitiesToCheck, area);
        }

        new PetSearchTask(sender, entitiesToCheck, targetUUID, area).runTaskTimer(plugin, 0L, 1L);
    }
