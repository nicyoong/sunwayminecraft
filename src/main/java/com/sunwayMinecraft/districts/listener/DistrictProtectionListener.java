package com.sunwayMinecraft.districts.listener;

import com.sunwayMinecraft.districts.config.DistrictSettingsConfig;
import com.sunwayMinecraft.districts.config.DistrictSettingsConfig.InteractionFlags;
import com.sunwayMinecraft.districts.domain.DistrictDefinition;
import com.sunwayMinecraft.districts.domain.DistrictType;
import com.sunwayMinecraft.districts.region.DistrictLocationResolver;
import com.sunwayMinecraft.districts.service.DistrictAlignmentService;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Enforces district rules in the world: build restrictions, interaction
 * restrictions, archived read-only behaviour and sanctuary safety.
 * Every check short-circuits when the matching enforcement switch is off.
 */
public class DistrictProtectionListener implements Listener {
    private static final Logger LOGGER = Logger.getLogger(DistrictProtectionListener.class.getName());

    private final JavaPlugin plugin;
    private final DistrictLocationResolver resolver;
    private final DistrictAlignmentService alignmentService;
    private final DistrictSettingsConfig settings;
    private final Supplier<CityMetrics> metrics;

    /** Minimal metrics surface so the city system stays optional. */
    public interface CityMetrics {
        void increment(String key);
    }

    public DistrictProtectionListener(
            JavaPlugin plugin,
            DistrictLocationResolver resolver,
            DistrictAlignmentService alignmentService,
            DistrictSettingsConfig settings,
            Supplier<CityMetrics> metrics) {
        this.plugin = plugin;
        this.resolver = resolver;
        this.alignmentService = alignmentService;
        this.settings = settings;
        this.metrics = metrics;
    }

    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // ───────────────────────── build ─────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!settings.isEnforceBuildRules()) {
            return;
        }
        DistrictDefinition district =
                resolver.getDistrictAt(event.getBlock().getLocation());
        if (district == null) {
            return;
        }
        if (alignmentService.canPlayerBuildInDistrict(event.getPlayer(), district)
                && !isSanctuaryBuildDenied(district, event.getPlayer())) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().sendMessage(buildDenialMessage(event.getPlayer(), district));
        metrics().ifPresent(m -> m.increment("district_build_denied"));
        logDenial("build", event.getPlayer(), district);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!settings.isEnforceBuildRules()) {
            return;
        }
        DistrictDefinition district =
                resolver.getDistrictAt(event.getBlock().getLocation());
        if (district == null) {
            return;
        }
        if (alignmentService.canPlayerBuildInDistrict(event.getPlayer(), district)
                && !isSanctuaryBuildDenied(district, event.getPlayer())) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().sendMessage(buildDenialMessage(event.getPlayer(), district));
        metrics().ifPresent(m -> m.increment("district_build_denied"));
        logDenial("break", event.getPlayer(), district);
    }

    // ───────────────────────── interaction ─────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!settings.isEnforceInteractionRules() || event.getClickedBlock() == null) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        DistrictDefinition district =
                resolver.getDistrictAt(event.getClickedBlock().getLocation());
        if (district == null) {
            return;
        }

        // both gates must pass: the player needs access AND the per-type flag
        // must allow the category (archived districts deny everything via
        // their flags, sanctuary districts deny containers and redstone)
        boolean accessAllowed =
                alignmentService.canPlayerAccessDistrict(event.getPlayer(), district);
        InteractionFlags flags = settings.interactionFlagsFor(district.getDistrictType());
        Material material = event.getClickedBlock().getType();
        if (accessAllowed && interactionAllowed(flags, material)) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().sendMessage(deniedInteractionMessage(district));
        metrics().ifPresent(m -> m.increment("district_interact_denied"));
        logDenial("interact", event.getPlayer(), district);
    }

    // ───────────────────────── sanctuary ─────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        DistrictDefinition district =
                resolver.getDistrictAt(victim.getLocation());
        if (district == null || district.getDistrictType() != DistrictType.SANCTUARY) {
            return;
        }

        Entity damager = event.getDamager();
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) {
            damager = shooter;
        }

        if (damager instanceof Player && settings.isSanctuaryNoPvp()) {
            event.setCancelled(true);
            if (damager instanceof Player attacker) {
                attacker.sendMessage("§cPvP is disabled in this sanctuary.");
            }
        } else if (damager instanceof Mob && settings.isSanctuaryNoMobDamage()) {
            event.setCancelled(true);
        }
    }

    // ───────────────────────── helpers ─────────────────────────

    private boolean isSanctuaryBuildDenied(DistrictDefinition district, Player player) {
        return district.getDistrictType() == DistrictType.SANCTUARY
                && settings.isSanctuaryNoBuild()
                && !player.hasPermission(DistrictAlignmentService.ADMIN_BYPASS_PERMISSION);
    }

    private boolean interactionAllowed(InteractionFlags flags, Material material) {
        return switch (material) {
            case CHEST, TRAPPED_CHEST, BARREL, FURNACE, BLAST_FURNACE, SMOKER,
                 HOPPER, DROPPER, DISPENSER, BREWING_STAND, SHULKER_BOX,
                 BLACK_SHULKER_BOX, BLUE_SHULKER_BOX, CYAN_SHULKER_BOX,
                 GRAY_SHULKER_BOX, LIME_SHULKER_BOX, PINK_SHULKER_BOX,
                 RED_SHULKER_BOX, WHITE_SHULKER_BOX, YELLOW_SHULKER_BOX -> flags.allowContainers();
            case LEVER, STONE_BUTTON, OAK_BUTTON, SPRUCE_BUTTON, BIRCH_BUTTON,
                 ACACIA_BUTTON, JUNGLE_BUTTON, DARK_OAK_BUTTON, CRIMSON_BUTTON,
                 WARPED_BUTTON, POLISHED_BLACKSTONE_BUTTON, STONE_PRESSURE_PLATE,
                 OAK_PRESSURE_PLATE, REPEATER, COMPARATOR, NOTE_BLOCK, DAYLIGHT_DETECTOR ->
                    flags.allowRedstone();
            case CRAFTING_TABLE, ANVIL, CHIPPED_ANVIL, DAMAGED_ANVIL,
                 ENCHANTING_TABLE, LOOM, CARTOGRAPHY_TABLE, GRINDSTONE, STONECUTTER ->
                    flags.allowCrafting();
            case OAK_DOOR, SPRUCE_DOOR, BIRCH_DOOR, JUNGLE_DOOR, ACACIA_DOOR,
                 DARK_OAK_DOOR, CRIMSON_DOOR, WARPED_DOOR, IRON_DOOR,
                 OAK_TRAPDOOR, SPRUCE_TRAPDOOR, BIRCH_TRAPDOOR, JUNGLE_TRAPDOOR,
                 ACACIA_TRAPDOOR, DARK_OAK_TRAPDOOR, IRON_TRAPDOOR,
                 OAK_FENCE_GATE, SPRUCE_FENCE_GATE, BIRCH_FENCE_GATE -> flags.allowDoors();
            default -> flags.allowInteract();
        };
    }

    private String buildDenialMessage(Player player, DistrictDefinition district) {
        if (district.isArchived() && settings.isArchivedReadOnly()) {
            return "§cThis archive district is read-only.";
        }
        if (district.getDistrictType() == DistrictType.SANCTUARY && settings.isSanctuaryNoBuild()) {
            return "§cYou cannot build in this sanctuary.";
        }
        if (!alignmentService.canPlayerAccessDistrict(player, district)) {
            return "§cThis district is controlled by another alignment.";
        }
        return "§cYou cannot build in this district.";
    }

    private String deniedInteractionMessage(DistrictDefinition district) {
        if (district.isArchived() && settings.isArchivedReadOnly()) {
            return "§cThis archive district is read-only.";
        }
        return "§cYou cannot interact in this district.";
    }

    private void logDenial(String action, Player player, DistrictDefinition district) {
        if (settings.isVerboseDenialLogging()) {
            LOGGER.info("[Districts] Denied " + action + " by " + player.getName()
                    + " in district '" + district.getId() + "'");
        }
    }

    private java.util.Optional<CityMetrics> metrics() {
        return java.util.Optional.ofNullable(metrics.get());
    }
}
