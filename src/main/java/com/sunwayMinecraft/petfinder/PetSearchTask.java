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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import java.util.*;

/**
 * The {@code PetSearchTask} class performs a batched search for tamed pets (dogs and cats)
 * within the Minecraft world, optionally filtering by owner and bounding area.
 *
 * <p>This task is scheduled via the Bukkit scheduler and processes entities in
 * configurable batch sizes to avoid blocking the main server thread.
 *
 * <p>Key responsibilities include:
 * <ul>
 *   <li>Filtering entities to valid, tamed pets owned by a specific player (or all owners if
 *       {@code targetUUID} is null).</li>
 *   <li>Restricting results to those within an optional {@link BoundingBox} area.</li>
 *   <li>Batch-processing a list of entities each run tick to spread workload.</li>
 *   <li>Tracking and logging chunk progress every ten unique chunks scanned.</li>
 *   <li>Aggregating and sending detailed search results, including pet counts and
 *       statuses, to the command sender upon completion.</li>
 * </ul>
 *
 * @see BukkitRunnable
 */
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

    /**
     * Constructs a new PetSearchTask to locate and report on tamed pets.
     *
     * @param plugin      the plugin instance for logging and scheduling tasks
     * @param sender      the command sender to notify with search results
     * @param entities    the initial list of entities to scan
     * @param targetUUID  the UUID of the pet owner to filter by (null for all owners)
     * @param area        optional bounding box to restrict search area (null for no restriction)
     * @param manager     the manager responsible for tracking search state
     * @param totalChunks the total count of chunks being scanned for progress reporting
     */
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

    /**
     * Executes one batch of entity processing per scheduler tick.
     *
     * <p>The method performs the following steps:
     * <ol>
     *   <li>Processes up to {@code batchSize} entities from the list.</li>
     *   <li>Filters out invalid entities, untamed pets, and those outside the search area.</li>
     *   <li>Invokes {@link #addToResults(Entity)} for each valid pet to record details.</li>
     *   <li>When the list is exhausted, sends final results via
     *       {@link #sendFinalResults()} and marks the search complete.</li>
     *   <li>Tracks the current chunk of the next entity, updating
     *       {@code processedChunks} and {@code lastChunks}.</li>
     *   <li>Logs progress every ten unique chunks scanned to the plugin logger.</li>
     * </ol>
     *
     * <p>This method runs on the server scheduler thread and must
     * complete quickly to avoid tick lag.
     */
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

    /**
     * Adds a detailed description of a valid pet to the results list,
     * including type, custom name (if any), current and max health,
     * world coordinates, and sitting state. Also increments the appropriate count.
     *
     * <p>This method performs:
     * <ol>
     *   <li>Determines pet type ({@code Dog} or {@code Cat}) and increments counters.</li>
     *   <li>Extracts and formats a custom name, defaulting to "Unnamed".</li>
     *   <li>Retrieves and rounds current and maximum health values.</li>
     *   <li>Checks if the pet is sitting via {@link Sittable}.</li>
     *   <li>Formats the combined details into a string and appends to {@code results}.</li>
     * </ol>
     *
     * @param pet the pet entity to describe
     */
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

        Component nameComponent = pet.customName();
        if (nameComponent != null) {
            String customName = PlainTextComponentSerializer.plainText().serialize(nameComponent);
            name = "§b" + customName;
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

    /**
     * Sends the aggregated search results to the command sender.
     *
     * <p>The method proceeds as follows:
     * <ol>
     *   <li>If no dogs or cats were found, sends a "No matching pets found" message.</li>
     *   <li>Otherwise, sends a summary count message indicating total pets,
     *       number of dogs, and number of cats.</li>
     *   <li>Sends each formatted pet detail line from {@code results}.</li>
     * </ol>
     *
     * <p>This provides users with both overview and detailed information in chat.
     */
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
