package com.sunwayMinecraft.containerfinder;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.List;

public class ContainerFinderManager {
    public static final int DEFAULT_RADIUS = 96;
    public static final int PAGE_SIZE = 10;
    public static final int CONTAINER_LIMIT = 2000;

    private final JavaPlugin plugin;
    private boolean searchRunning = false;
    private ContainerScanCache lastScanCache;

    public ContainerFinderManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public int getDefaultRadius() {
        return DEFAULT_RADIUS;
    }

    public boolean isSearchRunning() {
        return searchRunning;
    }
}
