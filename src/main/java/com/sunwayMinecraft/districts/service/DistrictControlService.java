package com.sunwayMinecraft.districts.service;

import com.sunwayMinecraft.districts.config.DistrictControlSettingsConfig;
import com.sunwayMinecraft.districts.config.DistrictsConfigManager;
import com.sunwayMinecraft.districts.domain.DistrictControlProfile;
import com.sunwayMinecraft.districts.domain.DistrictDefinition;
import com.sunwayMinecraft.districts.domain.DistrictOwnership;
import com.sunwayMinecraft.districts.event.DistrictControlChangeEvent;
import com.sunwayMinecraft.districts.persistence.DistrictControlRepository;
import com.sunwayMinecraft.districts.persistence.DistrictControlRepository.ControlStateRecord;
import com.sunwayMinecraft.districts.region.DistrictLocationResolver;
import net.kyori.adventure.text.Component;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Presence-based district control contests. Players inside a contest-enabled
 * district contribute points for their alignment; when one alignment erodes
 * the district's grip to zero it captures it. Control changes are persisted,
 * broadcast, recorded as history and announced via
 * {@link DistrictControlChangeEvent}.
 */
public class DistrictControlService {
    public enum ContestState { STABLE, CONTESTED, CAPTURING, COOLING_DOWN, LOCKED }

    private static final Logger LOGGER = Logger.getLogger(DistrictControlService.class.getName());

    /** Mutable runtime state for one district. */
    public class ControlState {
        public final String districtId;
        public String controllerAlignmentId;
        public String controllerGrandAllianceId;
        public ContestState state = ContestState.STABLE;
        public String leadingAlignmentId;
        public double progress;
        public long cooldownUntil;
        public boolean forceContest;
        public final Map<UUID, Double> contributors = new LinkedHashMap<>();

        ControlState(String districtId) {
            this.districtId = districtId;
        }

        public int getProgressPercent(int required) {
            return required <= 0 ? 0 : (int) Math.min(100, progress * 100 / required);
        }
    }

    private final JavaPlugin plugin;
    private final DistrictsConfigManager configManager;
    private final DistrictLocationResolver locationResolver;
    private final DistrictControlSettingsConfig settings;
    private final DistrictControlRepository repository;
    private final Function<UUID, Optional<String>> alignmentResolver;
    private final Supplier<Economy> economySupplier;
    private final Consumer<String> reputationAwarder;
    private final Supplier<Consumer<String>> metricsSupplier;
    private final Map<String, ControlState> states = new HashMap<>();
    private final Map<UUID, Long> lastRewardAt = new HashMap<>();
    private BukkitTask task;

    public DistrictControlService(
            JavaPlugin plugin,
            DistrictsConfigManager configManager,
            DistrictLocationResolver locationResolver,
            DistrictControlSettingsConfig settings,
            DistrictControlRepository repository,
            Function<UUID, Optional<String>> alignmentResolver,
            Supplier<Economy> economySupplier,
            Consumer<String> reputationAwarder,
            Supplier<Consumer<String>> metricsSupplier) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.locationResolver = locationResolver;
        this.settings = settings;
        this.repository = repository;
        this.alignmentResolver = alignmentResolver;
        this.economySupplier = economySupplier;
        this.reputationAwarder = reputationAwarder;
        this.metricsSupplier = metricsSupplier;
    }

    /** District lookup by id (delegates to the config manager). */
    public DistrictDefinition getDistrictById(String districtId) {
        return configManager.getDistrictById(districtId);
    }

    /** Effective district at a location (delegates to the location resolver). */
    public DistrictDefinition getDistrictAt(Location location) {
        return locationResolver.getDistrictAt(location);
    }

    /** Control settings access for command handlers. */
    public DistrictControlSettingsConfig getSettings() {
        return settings;
    }

    // ───────────────────── lifecycle ─────────────────────


    /** Starts the contest scheduler. */
    public void start() {
        stop();
        if (!settings.isEnabled()) {
            LOGGER.info("[Districts] District control is disabled by config");
            return;
        }
        long intervalTicks = settings.getContestTickIntervalSeconds() * 20L;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, intervalTicks, intervalTicks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    /** One scheduler pass: group players by district, then process each contest. */






















    /** Re-applies SQLite control state into the ownership override layer. */

    private void logAdmin(String actor, String action) {
        LOGGER.info("[Districts] [admin] " + actor + " " + action);
    }

    public Optional<ControlState> getControlState(String districtId) {
        return Optional.ofNullable(states.get(districtId));
    }

    /** Alignment id currently controlling a district, or null. */
    public String getControllerAlignment(String districtId) {
        ControlState state = states.get(districtId);
        return state == null ? null : state.controllerAlignmentId;
    }

    /** Number of districts currently controlled by an alignment. */
    public int getControlledCount(String alignmentId) {
        return (int) states.values().stream()
                .filter(state -> alignmentId != null && alignmentId.equals(state.controllerAlignmentId))
                .count();
    }

    public List<ControlState> getActiveContests() {
        List<ControlState> active = new ArrayList<>();
        for (ControlState state : states.values()) {
            if (state.state == ContestState.CONTESTED || state.state == ContestState.CAPTURING) {
                active.add(state);
            }
        }
        return active;
    }

    public DistrictControlRepository getRepository() {
        return repository;
    }

    private void persist(ControlState state, String reason) {
        boolean saved = repository.saveState(new ControlStateRecord(
                state.districtId, state.controllerAlignmentId, state.controllerGrandAllianceId,
                state.state.name(), state.leadingAlignmentId, (int) state.progress,
                System.currentTimeMillis()));
        if (!saved) {
            // keep runtime state; the next successful save will catch up
            LOGGER.warning("[Districts] Could not persist control state for '" + state.districtId
                    + "' (" + reason + "); runtime state kept");
        }
    }

    private void broadcast(String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
        }
    }

    private void fireControlChange(String districtId, String previous, String newAlignmentId,
                                   String allianceId, DistrictControlChangeEvent.ChangeReason reason) {
        DistrictDefinition district = configManager.getDistrict(districtId);
        Bukkit.getPluginManager().callEvent(new DistrictControlChangeEvent(
                district, previous, newAlignmentId, allianceId, reason));
    }

    private DistrictOwnership ownershipWithController(
            DistrictDefinition district, String alignmentId, String allianceId) {
        DistrictOwnership current = district == null ? null : district.getOwnership();
        if (current == null) {
            current = DistrictOwnership.neutral();
        }
        return new DistrictOwnership(current.homeCampus(), allianceId, alignmentId,
                current.allowedAlignments(), current.deniedAlignments(),
                current.propertyPolicy(), current.transitConnected(), current.contested());
    }


    private void sendContestActionBar(List<UUID> presentPlayers, ControlState state, int required) {
        String template = settings.getContestActionBar();
        for (UUID uuid : presentPlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendActionBar(Component.text(template
                        .replace("{alignment}", String.valueOf(state.leadingAlignmentId))
                        .replace("{percent}", String.valueOf(state.getProgressPercent(required)))));
            }
        }
    }

    private void transitionToContested(ControlState state, DistrictDefinition district,
                                       String leadingAlignmentId) {
        boolean fromStable = state.state == ContestState.STABLE;
        state.state = ContestState.CONTESTED;
        state.leadingAlignmentId = leadingAlignmentId;
        persist(state, "contested");
        if (fromStable && settings.isBroadcastContestStart()) {
            broadcast(settings.getContestStartMessage()
                    .replace("{district}", district.getDisplayName())
                    .replace("{alignment}", leadingAlignmentId == null ? "unknown" : leadingAlignmentId));
        }
        metrics().ifPresent(m -> m.accept("district_contests_started"));
    }

    private void neutralReset(ControlState state, DistrictDefinition district) {
        String previous = state.controllerAlignmentId;
        state.controllerAlignmentId = null;
        state.controllerGrandAllianceId = null;
        applyOwnership(state.districtId, null, null, previous,
                DistrictControlChangeEvent.ChangeReason.NEUTRAL_RESET);
        repository.appendHistory(new DistrictControlRepository.ControlHistoryRecord(
                0, state.districtId, previous, null,
                state.state.name(), System.currentTimeMillis(), "neutral_reset"));
        LOGGER.info("[Districts] District '" + state.districtId + "' decayed to neutral");
    }

    private void handleNoPresence(ControlState state, DistrictDefinition district) {
        boolean wasContesting = state.state == ContestState.CONTESTED
                || state.state == ContestState.CAPTURING;
        if (state.progress <= 0 && !wasContesting) {
            return;
        }
        if (!settings.isDecayWhenUncontested()) {
            return;
        }
        state.progress = Math.max(0, state.progress - settings.getPointsPerPlayerPerTick());
        if (state.progress <= 0) {
            if (wasContesting) {
                if (settings.isBroadcastContestEnd()) {
                    broadcast(settings.getContestEndMessage()
                            .replace("{district}", district.getDisplayName()));
                }
                metrics().ifPresent(m -> m.accept("district_contests_expired"));
            }
            state.state = ContestState.STABLE;
            state.leadingAlignmentId = null;
            state.contributors.clear();
            if (settings.isAllowNeutralReset() && state.controllerAlignmentId != null) {
                neutralReset(state, district);
            }
        }
        persist(state, "decay");
    }


    private java.util.Optional<Consumer<String>> metrics() {
        try {
            return java.util.Optional.ofNullable(metricsSupplier.get());
        } catch (NullPointerException | IllegalStateException e) {
            return java.util.Optional.empty();
        }
    }
}
