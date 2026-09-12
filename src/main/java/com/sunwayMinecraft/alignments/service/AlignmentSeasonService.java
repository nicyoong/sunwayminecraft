package com.sunwayMinecraft.alignments.service;

import com.sunwayMinecraft.alignments.config.AlignmentConfigManager;
import com.sunwayMinecraft.alignments.config.AlignmentProgressionConfig;
import com.sunwayMinecraft.alignments.domain.AlignmentDefinition;
import com.sunwayMinecraft.alignments.domain.AlignmentMembership;
import com.sunwayMinecraft.alignments.persistence.AlignmentRepository;
import com.sunwayMinecraft.alignments.persistence.AlignmentSeasonRepository;
import com.sunwayMinecraft.alignments.persistence.AlignmentSeasonRepository.AlignmentSnapshot;
import com.sunwayMinecraft.alignments.persistence.AlignmentSeasonRepository.PlayerSeasonRecord;
import com.sunwayMinecraft.alignments.persistence.AlignmentSeasonRepository.SeasonState;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Runs alignment seasons: tracks the current season in SQLite, snapshots
 * alignment and player reputation when a season ends, optionally resets
 * reputation, and broadcasts the results.
 */
public class AlignmentSeasonService {
  private static final Logger LOGGER = Logger.getLogger(AlignmentSeasonService.class.getName());

  private final JavaPlugin plugin;
  private final AlignmentProgressionConfig progressionConfig;
  private final AlignmentSeasonRepository seasonRepository;
  private final AlignmentRepository membershipRepository;
  private final AlignmentConfigManager configManager;
  private final AlignmentMembershipCache cache;

  public AlignmentSeasonService(
      JavaPlugin plugin,
      AlignmentProgressionConfig progressionConfig,
      AlignmentSeasonRepository seasonRepository,
      AlignmentRepository membershipRepository,
      AlignmentConfigManager configManager,
      AlignmentMembershipCache cache) {
    this.plugin = plugin;
    this.progressionConfig = progressionConfig;
    this.seasonRepository = seasonRepository;
    this.membershipRepository = membershipRepository;
    this.configManager = configManager;
    this.cache = cache;
  }

  /** Loads or creates season state; closes a pending season if configured. */
  public void start() {
    if (!seasonRepository.isAvailable()) {
      plugin.getLogger().warning("Season storage unavailable; alignment seasons are disabled");
      return;
    }
    Optional<SeasonState> state = seasonRepository.getSeasonState();
    if (state.isEmpty()) {
      startNewSeason("season-1");
      return;
    }
    if (progressionConfig.getRestartBehavior()
            == AlignmentProgressionConfig.RestartBehavior.END_PENDING_ON_ENABLE
        && isEnded(state.get())) {
      endSeason("pending on enable");
    }
  }

  /** Periodic check (hourly task) that closes a season once it has ended. */
  public void checkSeason() {
    seasonRepository.getSeasonState()
        .filter(this::isEnded)
        .ifPresent(state -> endSeason("scheduled"));
  }

  public boolean isEnded(SeasonState state) {
    long length = progressionConfig.getSeasonLengthMillis();
    return length > 0 && System.currentTimeMillis() >= state.startedAt() + length;
  }

  public Optional<SeasonState> getCurrentSeason() {
    return seasonRepository.getSeasonState();
  }

  /**
   * Snapshots the current season, optionally resets reputations, opens the
   * next season and broadcasts the results.
   */
  public boolean endSeason(String reason) {
    if (!seasonRepository.isAvailable()) {
      LOGGER.warning("Cannot end season: season storage is unavailable");
      return false;
    }
    String seasonId = seasonRepository.getSeasonState()
        .map(SeasonState::seasonId)
        .orElse("season-1");

    List<AlignmentSnapshot> snapshots = snapshotCurrent(seasonId);
    if (progressionConfig.isResetReputationOnEnd()) {
      int reset = seasonRepository.resetAllReputations();
      cache.clearAll();
      cache.loadOnlinePlayers();
      LOGGER.info("Season " + seasonId + ": reset reputation for " + reset + " members");
    }
    String nextSeasonId = nextSeasonId(seasonId);
    seasonRepository.saveSeasonState(
        new SeasonState(nextSeasonId, System.currentTimeMillis()));
    LOGGER.info("Season " + seasonId + " ended (" + reason + "); started " + nextSeasonId);

    if (progressionConfig.isBroadcastResults()) {
      broadcastResults(seasonId, snapshots);
    }
    return true;
  }

  /** Starts a fresh season without snapshotting or resetting reputations. */
  public boolean resetSeason() {
    if (!seasonRepository.isAvailable()) {
      LOGGER.warning("Cannot reset season: season storage is unavailable");
      return false;
    }
    String current = seasonRepository.getSeasonState()
        .map(SeasonState::seasonId)
        .orElse("season-1");
    String nextSeasonId = nextSeasonId(current);
    boolean saved = seasonRepository.saveSeasonState(
        new SeasonState(nextSeasonId, System.currentTimeMillis()));
    if (saved) {
      LOGGER.info("Season " + current + " was reset by admin; started " + nextSeasonId);
    }
    return saved;
  }

  /** Snapshots the current season without ending it (admin action). */
  public boolean forceSnapshot() {
    if (!seasonRepository.isAvailable()) {
      LOGGER.warning("Cannot snapshot season: season storage is unavailable");
      return false;
    }
    String seasonId = seasonRepository.getSeasonState()
        .map(SeasonState::seasonId)
        .orElse("season-1");
    List<AlignmentSnapshot> snapshots = snapshotCurrent(seasonId);
    LOGGER.info("Manual snapshot of season " + seasonId + " recorded " + snapshots.size()
        + " alignment entries");
    return true;
  }

  /**
   * Reputation totals per alignment for the current season. Recorded
   * season data only exists once a snapshot ran, so the view falls back to
   * the live membership totals - the current season's standings are simply
   * everyone's reputation right now.
   */
  public Map<String, long[]> getSeasonAlignmentTotals() {
    String seasonId = seasonRepository.getSeasonState()
        .map(SeasonState::seasonId)
        .orElse(null);
    if (seasonId == null) {
      return Map.of();
    }
    Map<String, long[]> recorded = seasonRepository.getSeasonAlignmentTotals(seasonId);
    return recorded.isEmpty() ? membershipRepository.getAlignmentTotals() : recorded;
  }

  private List<AlignmentSnapshot> snapshotCurrent(String seasonId) {
    long now = System.currentTimeMillis();
    Map<String, long[]> totals = membershipRepository.getAlignmentTotals();
    List<AlignmentSnapshot> snapshots = new ArrayList<>();
    for (Map.Entry<String, long[]> entry : totals.entrySet()) {
      String allianceId = configManager.getAlignment(entry.getKey())
          .map(def -> def.grandAlliance().getId())
          .orElse("unknown");
      snapshots.add(new AlignmentSnapshot(
          0, seasonId, entry.getKey(), allianceId, entry.getValue()[0],
          (int) entry.getValue()[1], now));
    }
    if (!snapshots.isEmpty() && !seasonRepository.insertSnapshots(snapshots)) {
      LOGGER.warning("Failed to persist alignment snapshots for season " + seasonId);
    }

    List<PlayerSeasonRecord> records = new ArrayList<>();
    for (AlignmentMembership membership : membershipRepository.getAllMemberships()) {
      records.add(new PlayerSeasonRecord(
          seasonId, membership.playerUuid(), membership.alignmentId(),
          membership.reputation(), now));
    }
    if (!records.isEmpty() && !seasonRepository.insertPlayerRecords(records)) {
      LOGGER.warning("Failed to persist player season records for season " + seasonId);
    }
    return snapshots;
  }

  private void startNewSeason(String seasonId) {
    seasonRepository.saveSeasonState(new SeasonState(seasonId, System.currentTimeMillis()));
    LOGGER.info("Started alignment season " + seasonId);
  }

  private String nextSeasonId(String current) {
    String prefix = "season-";
    if (current.startsWith(prefix)) {
      try {
        return prefix + (Integer.parseInt(current.substring(prefix.length())) + 1);
      } catch (NumberFormatException ignored) {
        // not a numbered season id; fall through to the suffix
      }
    }
    return current + "-next";
  }

  private void broadcastResults(String seasonId, List<AlignmentSnapshot> snapshots) {
    List<AlignmentSnapshot> top = snapshots.stream()
        .sorted(Comparator.comparingLong(AlignmentSnapshot::totalReputation).reversed())
        .limit(3)
        .toList();
    if (top.isEmpty()) {
      return;
    }
    StringBuilder message = new StringBuilder("§6=== Season " + seasonId + " Results ===");
    int place = 1;
    for (AlignmentSnapshot snapshot : top) {
      message.append("\n§e#").append(place++).append(" §f").append(snapshot.alignmentId())
          .append(" §7- §e").append(snapshot.totalReputation()).append(" reputation §7(")
          .append(snapshot.memberCount()).append(" members)");
    }
    String text = message.toString();
    for (Player player : Bukkit.getOnlinePlayers()) {
      player.sendMessage(text);
    }
    LOGGER.info(text.replaceAll("§.", ""));
  }
}
