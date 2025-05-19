package com.sunwayMinecraft.petfinder;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import java.util.*;

public class PetSearchTask extends BukkitRunnable {
    private final CommandSender sender;
    private final List<Entity> entities;
    private final UUID targetUUID;
    private final BoundingBox area;
    private final PetFinderManager manager;
    private final List<String> results = new ArrayList<>();

    public PetSearchTask(Main plugin, CommandSender sender, List<Entity> entities,
                         UUID targetUUID, BoundingBox area, PetFinderManager manager) {
        this.sender = sender;
        this.entities = new ArrayList<>(entities);
        this.targetUUID = targetUUID;
        this.area = area;
        this.manager = manager;
    }
}
