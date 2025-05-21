package com.sunwayMinecraft.petfinder;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Sittable;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.entity.LivingEntity;
import org.bukkit.attribute.Attribute;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import java.util.*;

// this class should be tested
public class PetSearchTask extends BukkitRunnable {
    private final JavaPlugin plugin;
    private final CommandSender sender;
    private final List<Entity> entities;
    private final UUID targetUUID;
    private final BoundingBox area;
    private final PetFinderManager manager;
    private final List<String> results = new ArrayList<>();
    private int totalChunks;
    private int processedChunks = 0;
    private final LinkedList<String> lastChunks = new LinkedList<>();
    private int dogCount = 0;
    private int catCount = 0;

    public PetSearchTask(JavaPlugin plugin, CommandSender sender, List<Entity> entities,
                          UUID targetUUID, BoundingBox area, PetFinderManager manager,
                          int totalChunks) {
        this.plugin = plugin;
        this.sender = sender;
        this.entities = new ArrayList<>(entities);
        this.targetUUID = targetUUID;
        this.area = area;
        this.manager = manager;
        this.totalChunks = totalChunks;
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

        if (!entities.isEmpty()) {
            Entity entity = entities.get(0);
            Location loc = entity.getLocation();
            String chunkID = String.format("%s:%d,%d",
                    loc.getWorld().getName(),
                    loc.getBlockX() >> 4,  // Chunk X
                    loc.getBlockZ() >> 4   // Chunk Z
            );

            if (!lastChunks.contains(chunkID)) {
                lastChunks.addFirst(chunkID);
                if (lastChunks.size() > 10) lastChunks.removeLast();
                processedChunks++;
            }
        }

        // Log every 10 chunks
        if (processedChunks % 10 == 0 && !lastChunks.isEmpty()) {
            plugin.getLogger().info("Total chunks: " + totalChunks);
            plugin.getLogger().info("Recent chunks: " + String.join(", ", lastChunks));
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

    private boolean isInArea(Location loc) {
        return area.contains(loc.getX(), loc.getY(), loc.getZ());
    }

    private void addToResults(Entity pet) {
        Location loc = pet.getLocation();
        String type;
        String name = "§fUnnamed";

        // Count specific types
        if (pet instanceof Wolf) {
            type = "Dog";
            dogCount++;
        } else if (pet instanceof Cat) {
            type = "Cat";
            catCount++;
        } else {
            return; // Should never happen with our filters
        }

        // Get pet name
        if (pet.getCustomName() != null) {
            name = "§b" + pet.getCustomName();
        }

        LivingEntity livingPet = (LivingEntity) pet;
        double currentHealth = Math.round(livingPet.getHealth() * 10) / 10.0;
        double maxHealth = Math.round(livingPet.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() * 10) / 10.0;
        String health = String.format("Health: §7%.1f/%.1f", currentHealth, maxHealth);

        boolean sitting = pet instanceof Sittable && ((Sittable) pet).isSitting();

        results.add(String.format("§7- §e%s §8- %s §7- %s §7at §b%s §7(%s§7)",
                type, name, health, formatLocation(loc), sitting ? "§cSitting" : "§aStanding"));
    }

    private String formatLocation(Location loc) {
        return String.format("World: §6%s §7X: §b%.0f §7Y: §b%.0f §7Z: §b%.0f",
                loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
    }

    private void sendFinalResults() {
        if (dogCount == 0 && catCount == 0) {
            sender.sendMessage("§eNo matching pets found.");
        } else {
            // New formatted count message
            String countMessage = String.format(
                    "§aFound §e%d §apets: §e%d §adogs and §e%d §acats:",
                    dogCount + catCount,
                    dogCount,
                    catCount
            );

            sender.sendMessage(countMessage);
            results.forEach(sender::sendMessage);
        }
    }
}
