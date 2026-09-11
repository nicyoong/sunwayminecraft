package com.sunwayMinecraft.alignments.service;

import com.sunwayMinecraft.alignments.config.AlignmentProgressionConfig;
import com.sunwayMinecraft.alignments.config.AlignmentProgressionConfig.RankDefinition;

import java.util.Optional;

/**
 * Resolves a player's reputation rank. Per-grand-alliance rank sets win over
 * the global set; a broken config falls back to the lowest enabled rank.
 */
public class AlignmentRankService {
  private final AlignmentProgressionConfig progressionConfig;

  public AlignmentRankService(AlignmentProgressionConfig progressionConfig) {
    this.progressionConfig = progressionConfig;
  }

  /** Highest enabled rank the reputation reaches, or the safe fallback. */
  public Optional<RankDefinition> resolveRank(int reputation, String grandAllianceId) {
    return progressionConfig.resolveRank(reputation, grandAllianceId);
  }

  /** Rank id for cache storage, or null when no rank can be resolved. */
  public String resolveRankId(int reputation, String grandAllianceId) {
    return progressionConfig.resolveRank(reputation, grandAllianceId)
        .map(RankDefinition::id)
        .orElse(null);
  }

  /** Reputation threshold a player must reach to hold the given rank. */
  public int minimumReputationFor(String grandAllianceId, String rankId) {
    return progressionConfig.getRank(grandAllianceId, rankId)
        .map(RankDefinition::minimumReputation)
        .orElse(Integer.MAX_VALUE);
  }

  /** True when the reputation meets the rank's threshold. */
  public boolean meetsRank(int reputation, String grandAllianceId, String requiredRankId) {
    return reputation >= minimumReputationFor(grandAllianceId, requiredRankId);
  }
}
