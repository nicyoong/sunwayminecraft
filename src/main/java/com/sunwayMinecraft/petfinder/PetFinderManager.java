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

/**
 * Manages the lifecycle of pet searches for tamed wolves and cats in a Minecraft plugin.
 *
 * <p>This class is responsible for initiating and managing searches for tamed {@link Wolf}
 * and {@link Cat} entities across all loaded worlds, optionally within a specified
 * {@link BoundingBox}. It prevents concurrent searches and schedules a {@link PetSearchTask}
 * to process the found entities.</p>
 *
 * <p>The manager provides functionality to:
 * <ul>
 *     <li>Start a new pet search with owner filtering by {@code targetUUID}.</li>
 *     <li>Optionally restrict the search to a defined region.</li>
 *     <li>Prevent overlapping searches by tracking the search state.</li>
 *     <li>Mark searches as complete to allow subsequent searches.</li>
 * </ul>
 * </p>
 *
 * <p>Field summary:
 * <ul>
 *     <li>{@link JavaPlugin} plugin: plugin instance for scheduling tasks and accessing APIs.</li>
 *     <li>{@code isSearchRunning}: tracks whether a search is in progress.</li>
 * </ul>
 * </p>
 *
 * <p>Methods:
 * <ul>
 *     <li>{@link #startSearch(CommandSender, UUID, BoundingBox)}: Begins a pet search.</li>
 *     <li>{@link #setSearchComplete()}: Marks the search as finished.</li>
 *     <li>{@link #addEntitiesByType(World, Class, List, BoundingBox)}: Gathers entities of a specific type.</li>
 * </ul>
 * </p>
 */
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

        Set<String> uniqueChunks = new HashSet<>();
        for (Entity entity : entitiesToCheck) {
            Location loc = entity.getLocation();
            uniqueChunks.add(String.format("%s:%d,%d",
                    loc.getWorld().getName(),
                    loc.getBlockX() >> 4,
                    loc.getBlockZ() >> 4
            ));
        }

        new PetSearchTask(plugin, sender, new ArrayList<>(entitiesToCheck), targetUUID, area, this,
                uniqueChunks.size())
                .runTaskTimer(plugin, 0L, 1L);
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
