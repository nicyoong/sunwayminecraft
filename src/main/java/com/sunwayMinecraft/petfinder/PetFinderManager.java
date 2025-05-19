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
}
