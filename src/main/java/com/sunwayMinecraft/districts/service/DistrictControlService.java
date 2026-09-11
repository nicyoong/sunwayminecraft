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


    public void tick() {
        if (!settings.isEnabled()) {
            return;
        }
        Map<String, Map<String, Integer>> presence = new HashMap<>();
        Map<String, List<UUID>> presentPlayers = new HashMap<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }
            Optional<String> alignment = alignmentResolver.apply(player.getUniqueId());
            if (alignment.isEmpty()) {
                continue; // unaligned players never count towards control
            }
            for (DistrictDefinition district : locationResolver.getDistrictsAt(player.getLocation())) {
                if (!isContestActive(district)) {
                    continue;
                }
                presence.computeIfAbsent(district.getId(), k -> new HashMap<>())
                        .merge(alignment.get(), 1, Integer::sum);
                presentPlayers.computeIfAbsent(district.getId(), k -> new ArrayList<>())
                        .add(player.getUniqueId());
            }
        }

        Set<String> processed = new HashSet<>(presence.keySet());
        processed.addAll(states.keySet());
        for (String districtId : processed) {
            try {
                processDistrict(districtId, presence.get(districtId), presentPlayers.get(districtId));
            } catch (Exception e) {
                LOGGER.severe("[Districts] Control tick failed for district '" + districtId
                        + "': " + e.getMessage());
            }
        }
    }
    private boolean isContestActive(DistrictDefinition district) {
        DistrictControlProfile profile = configManager.getControlProfile(district.getId());
        if (profile.contestEnabled()) {
            return true;
        }
        ControlState state = states.get(district.getId());
        return state != null && state.forceContest;
    }

    private void processDistrict(String districtId, Map<String, Integer> presence,
                                 List<UUID> presentPlayers) {
        DistrictDefinition district = configManager.getDistrict(districtId);
        if (district == null || !district.isEnabled()) {
            return;
        }
        DistrictControlProfile profile = configManager.getControlProfile(districtId);
        ControlState state = states.computeIfAbsent(districtId, ControlState::new);

        if (state.state == ContestState.LOCKED) {
            return;
        }
        int required = profile.pointsRequiredToCapture() > 0
                ? profile.pointsRequiredToCapture() : settings.getPointsRequiredToCapture();
        int totalPresence = presence == null ? 0 : presence.values().stream().mapToInt(Integer::intValue).sum();

        if (state.state == ContestState.COOLING_DOWN) {
            if (totalPresence >= settings.getMinimumPlayersToContest()) {
                return; // presence protects the district while it cools down
            }
            handleNoPresence(state, district); // the grip decays even while cooling down
            if (state.state == ContestState.COOLING_DOWN
                    && System.currentTimeMillis() >= state.cooldownUntil) {
                state.state = ContestState.STABLE;
                persist(state, "cooldown_elapsed");
            }
            return;
        }
        boolean contestActive = profile.contestEnabled() || state.forceContest;
        if (!contestActive) {
            return;
        }
        if (totalPresence < settings.getMinimumPlayersToContest()) {
            handleNoPresence(state, district);
            return;
        }

        if (presence.size() > 1) {
            // multiple alignments present: the contest is paused, nobody scores
            if (state.state != ContestState.CONTESTED) {
                transitionToContested(state, district, null);
            }
            sendContestActionBar(presentPlayers, state, required);
            return;
        }

        String leading = presence.keySet().iterator().next();
        double points = settings.getPointsPerPlayerPerTick() * presence.get(leading);

        if (leading.equals(state.controllerAlignmentId)) {
            // the controller maintains its grip while present
            state.state = ContestState.STABLE;
            state.leadingAlignmentId = null;
            state.progress = Math.min(settings.getMaximumControlPoints(), state.progress + points);
            persist(state, "controller_holding");
            return;
        }

        if (state.controllerAlignmentId != null) {
            // an enemy alignment erodes the controller's grip; the district
            // flips when the grip breaks
            state.state = ContestState.CAPTURING;
            state.leadingAlignmentId = leading;
            state.progress -= points;
            for (UUID contributor : presentPlayers) {
                state.contributors.merge(contributor, points, Double::sum);
            }
            persist(state, "eroding");
            sendContestActionBar(presentPlayers, state, required);
            if (state.progress <= 0) {
                capture(state, district, leading, required);
            }
            return;
        }

        // neutral district: capture progress builds toward the threshold
        if (state.state != ContestState.CAPTURING) {
            transitionToContested(state, district, leading);
        }
        state.state = ContestState.CAPTURING;
        state.leadingAlignmentId = leading;
        state.progress = Math.min(settings.getMaximumControlPoints(), state.progress + points);
        for (UUID contributor : presentPlayers) {
            state.contributors.merge(contributor, points, Double::sum);
        }
        persist(state, "capturing");
        sendContestActionBar(presentPlayers, state, required);

        if (state.progress >= required) {
            capture(state, district, leading, required);
        }
    }

    private void capture(ControlState state, DistrictDefinition district, String newAlignmentId,
                         int required) {
        String previous = state.controllerAlignmentId;
        String alliance = configManager.allianceOfAlignment(newAlignmentId);
        state.controllerAlignmentId = newAlignmentId;
        state.controllerGrandAllianceId = alliance;
        state.state = ContestState.COOLING_DOWN;
        int cooldownSeconds = configManager.getControlProfile(district.getId()).contestCooldownSeconds();
        if (cooldownSeconds <= 0) {
            cooldownSeconds = settings.getCaptureCooldownSeconds();
        }
        state.cooldownUntil = System.currentTimeMillis() + cooldownSeconds * 1000L;
        state.progress = required;
        persist(state, "captured");
        repository.appendHistory(new DistrictControlRepository.ControlHistoryRecord(
                0, state.districtId, previous, newAlignmentId,
                state.state.name(), System.currentTimeMillis(), "captured"));
        applyOwnership(state.districtId, newAlignmentId, alliance, previous,
                DistrictControlChangeEvent.ChangeReason.CAPTURE);
        if (settings.isBroadcastCapture()) {
            broadcast(settings.getCaptureMessage()
                    .replace("{district}", district.getDisplayName())
                    .replace("{alignment}", configManager.alignmentDisplayName(newAlignmentId))
                    .replace("{alliance}", alliance == null ? "unknown" : alliance));
        }
        metrics().ifPresent(m -> m.accept("district_captures"));
        rewardContributors(state, district, newAlignmentId);
        state.contributors.clear();
        LOGGER.info("[Districts] District '" + state.districtId + "' captured by " + newAlignmentId);
    }

    private void rewardContributors(ControlState state, DistrictDefinition district,
                                    String newAlignmentId) {
        if (!settings.isRewardControllersOnCapture()) {
            state.contributors.clear();
            return;
        }
        Economy economy = economySupplier.get();
        boolean payMoney = economy != null && settings.getRewardMoney() > 0;
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Double> entry : state.contributors.entrySet()) {
            if (entry.getValue() < settings.getMinimumContributionPoints()) {
                continue;
            }
            Long lastReward = lastRewardAt.get(entry.getKey());
            if (lastReward != null
                    && now - lastReward < settings.getRewardCooldownSeconds() * 1000L) {
                continue;
            }
            Player contributor = Bukkit.getPlayer(entry.getKey());
            if (contributor == null) {
                continue;
            }
            lastRewardAt.put(entry.getKey(), now);
            if (payMoney && settings.isRewardPlayersPresent()) {
                economy.depositPlayer(contributor, settings.getRewardMoney());
                contributor.sendMessage("§aCapture reward: §e" + settings.getRewardMoney());
            }
        }
        if (settings.getRewardReputation() > 0 && reputationAwarder != null) {
            reputationAwarder.accept(newAlignmentId);
        }
        state.contributors.clear();
    }


    private java.util.Optional<Consumer<String>> metrics() {
        try {
            return java.util.Optional.ofNullable(metricsSupplier.get());
        } catch (NullPointerException | IllegalStateException e) {
            return java.util.Optional.empty();
        }
    }
}
