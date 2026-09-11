package com.sunwayMinecraft.alignments.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Reputation ranks, season settings and score weights, loaded from
 * alignment-progression.yml. Invalid entries are skipped with a logged
 * warning; the lowest enabled rank acts as the safe fallback.
 */
public class AlignmentProgressionConfig {
  /** What happens to a season that ended while the server was offline. */
  public enum RestartBehavior { CONTINUE, END_PENDING_ON_ENABLE }

  /** Weights for the grand alliance score formula. */
  public record ScoreWeights(int reputation, int member, int district, int contract) {}

  /** A reputation rank: id, display data and the threshold to reach it. */
  public record RankDefinition(
      String id, String displayName, int minimumReputation, String chatSuffix,
      String description, boolean enabled) {}

  private final JavaPlugin plugin;
  private final File configFile;

  private final Map<String, RankDefinition> globalRanks = new HashMap<>();
  private final Map<String, Map<String, RankDefinition>> allianceRanks = new HashMap<>();
  private long seasonLengthMillis = 0;
  private RestartBehavior restartBehavior = RestartBehavior.END_PENDING_ON_ENABLE;
  private boolean resetReputationOnEnd = false;
  private boolean broadcastResults = true;
  private ScoreWeights scoreWeights = new ScoreWeights(1, 2, 25, 1);

  public AlignmentProgressionConfig(JavaPlugin plugin) {
    this.plugin = plugin;
    this.configFile = new File(plugin.getDataFolder(), "alignment-progression.yml");
    if (!configFile.exists()) {
      plugin.saveResource("alignment-progression.yml", false);
    }
  }

  public void load() {
    globalRanks.clear();
    allianceRanks.clear();
    YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
    loadRanks(config.getConfigurationSection("ranks"), globalRanks, "global");
    loadAllianceRanks(config.getConfigurationSection("alliance_ranks"));
    loadSeason(config.getConfigurationSection("season"));
    loadScoreWeights(config.getConfigurationSection("score_weights"));

    if (globalRanks.isEmpty()) {
      plugin.getLogger().warning("No valid ranks in alignment-progression.yml; "
          + "every player will resolve to no rank until this is fixed");
    }
  }

  private void loadRanks(ConfigurationSection section, Map<String, RankDefinition> target,
      String label) {
    if (section == null) {
      if ("global".equals(label)) {
        plugin.getLogger().warning("alignment-progression.yml is missing the ranks section");
      }
      return;
    }
    for (String key : section.getKeys(false)) {
      String displayName = section.getString(key + ".display_name");
      if (displayName == null || displayName.isBlank()) {
        plugin.getLogger().warning("Rank " + key + " (" + label + ") is missing display_name; skipped");
        continue;
      }
      if (target.containsKey(key)) {
        plugin.getLogger().warning("Duplicate rank id " + key + " (" + label + "); skipped");
        continue;
      }
      int minimum = section.getInt(key + ".minimum_reputation", 0);
      if (minimum < 0) {
        plugin.getLogger().warning("Rank " + key + " (" + label + ") has a negative threshold; "
            + "using 0");
        minimum = 0;
      }
      target.put(key, new RankDefinition(
          key,
          displayName,
          minimum,
          section.getString(key + ".chat_suffix", ""),
          section.getString(key + ".description", ""),
          section.getBoolean(key + ".enabled", true)));
    }
  }

  private void loadAllianceRanks(ConfigurationSection section) {
    if (section == null) return;
    for (String allianceId : section.getKeys(false)) {
      Map<String, RankDefinition> overrides = new HashMap<>();
      loadRanks(section.getConfigurationSection(allianceId), overrides, allianceId);
      if (!overrides.isEmpty()) {
        allianceRanks.put(allianceId, overrides);
      }
    }
  }

  private void loadSeason(ConfigurationSection section) {
    if (section == null) return;
    long days = Math.max(0, section.getLong("length_days", 30));
    long hours = Math.max(0, section.getLong("length_hours", 0));
    seasonLengthMillis = (days * 24 + hours) * 3_600_000L;
    String behavior = section.getString("restart_behavior", "end_pending_on_enable");
    try {
      restartBehavior = RestartBehavior.valueOf(
          behavior.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
    } catch (IllegalArgumentException e) {
      plugin.getLogger().warning("Invalid restart_behavior '" + behavior
          + "' in alignment-progression.yml; using end_pending_on_enable");
      restartBehavior = RestartBehavior.END_PENDING_ON_ENABLE;
    }
    resetReputationOnEnd = section.getBoolean("reset_reputation_on_end", false);
    broadcastResults = section.getBoolean("broadcast_results", true);
  }

  private void loadScoreWeights(ConfigurationSection section) {
    if (section == null) return;
    scoreWeights = new ScoreWeights(
        Math.max(0, section.getInt("reputation", 1)),
        Math.max(0, section.getInt("member", 2)),
        Math.max(0, section.getInt("district", 25)),
        Math.max(0, section.getInt("contract", 1)));
  }

  /** Ranks for a grand alliance (its overrides when present, else global). */
  public List<RankDefinition> getRanks(String grandAllianceId) {
    Map<String, RankDefinition> source =
        grandAllianceId != null ? allianceRanks.getOrDefault(grandAllianceId, globalRanks) : globalRanks;
    List<RankDefinition> ranks = new ArrayList<>(source.values());
    ranks.sort(Comparator.comparingInt(RankDefinition::minimumReputation));
    return ranks;
  }

  /**
   * Highest enabled rank whose threshold the reputation reaches. Falls back
   * to the lowest enabled rank when the config is broken or nothing matches.
   */
  public Optional<RankDefinition> resolveRank(int reputation, String grandAllianceId) {
    RankDefinition fallback = null;
    RankDefinition best = null;
    for (RankDefinition rank : getRanks(grandAllianceId)) {
      if (!rank.enabled()) continue;
      if (fallback == null || rank.minimumReputation() < fallback.minimumReputation()) {
        fallback = rank;
      }
      if (reputation >= rank.minimumReputation()
          && (best == null || rank.minimumReputation() >= best.minimumReputation())) {
        best = rank;
      }
    }
    return Optional.ofNullable(best != null ? best : fallback);
  }

  public Optional<RankDefinition> getRank(String grandAllianceId, String rankId) {
    Map<String, RankDefinition> source =
        grandAllianceId != null ? allianceRanks.getOrDefault(grandAllianceId, globalRanks) : globalRanks;
    return Optional.ofNullable(source.get(rankId)).filter(RankDefinition::enabled);
  }

  public boolean hasRanks() {
    return !globalRanks.isEmpty();
  }

  public long getSeasonLengthMillis() {
    return seasonLengthMillis;
  }

  public RestartBehavior getRestartBehavior() {
    return restartBehavior;
  }

  public boolean isResetReputationOnEnd() {
    return resetReputationOnEnd;
  }

  public boolean isBroadcastResults() {
    return broadcastResults;
  }

  public ScoreWeights getScoreWeights() {
    return scoreWeights;
  }
}
