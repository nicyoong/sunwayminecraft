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

    @Override
    public void run() {
        int batchSize = 50;
        int processed = 0;

        while (processed < batchSize && !entities.isEmpty()) {
            Entity entity = entities.remove(0);
            processed++;

            if (!isValidPet(entity)) continue;
            if (area != null && !isInArea(entity.getLocation())) continue;

            addToResults(entity);
        }

        if (entities.isEmpty()) {
            sendFinalResults();
            manager.setSearchComplete();
            this.cancel();
        }
    }

    private boolean isValidPet(Entity entity) {
        if (entity.isDead() || !entity.isValid()) return false;
        if (!(entity instanceof Tameable)) return false;

        Tameable pet = (Tameable) entity;
        return pet.isTamed() &&
                pet.getOwner() != null &&
                (targetUUID == null || pet.getOwner().getUniqueId().equals(targetUUID));
    }
}
