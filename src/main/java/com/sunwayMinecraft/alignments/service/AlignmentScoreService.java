package com.sunwayMinecraft.alignments.service;

import com.sunwayMinecraft.alignments.config.AlignmentConfigManager;
import com.sunwayMinecraft.alignments.config.AlignmentProgressionConfig;
import com.sunwayMinecraft.alignments.persistence.AlignmentRepository;

import java.util.HashMap;
import java.util.Map;

/**
 * Grand alliance scoring. Score = reputation_weight * total_reputation
 * + member_weight * member_count. District control and contract completion
 * weights exist in the config but currently always contribute 0: the
 * district system has no ownership concept and city metrics only track
 * global counters, so there is no per-alliance data to use. When either
 * system gains per-alliance data, add the term here.
 */
public class AlignmentScoreService {
  private final AlignmentProgressionConfig progressionConfig;
  private final AlignmentConfigManager configManager;
  private final AlignmentRepository membershipRepository;

  public AlignmentScoreService(
      AlignmentProgressionConfig progressionConfig,
      AlignmentConfigManager configManager,
      AlignmentRepository membershipRepository) {
    this.progressionConfig = progressionConfig;
    this.configManager = configManager;
    this.membershipRepository = membershipRepository;
  }

  /** Weighted score for one grand alliance across all its alignments. */
  public long getGrandAllianceScore(String grandAllianceId) {
    AlignmentProgressionConfig.ScoreWeights weights = progressionConfig.getScoreWeights();
    long reputation = 0;
    long members = 0;
    for (Map.Entry<String, long[]> entry : membershipRepository.getAlignmentTotals().entrySet()) {
      if (configManager.getAlignment(entry.getKey())
          .filter(def -> def.grandAlliance().getId().equals(grandAllianceId))
          .isEmpty()) {
        continue;
      }
      reputation += entry.getValue()[0];
      members += entry.getValue()[1];
    }
    return (long) weights.reputation() * reputation
        + (long) weights.member() * members
        + (long) weights.district() * districtsHeld(grandAllianceId)
        + (long) weights.contract() * contractsCompleted(grandAllianceId);
  }

  /** Scores for every grand alliance, sorted descending on read. */
  public Map<String, Long> getGrandAllianceScores() {
    Map<String, Long> scores = new HashMap<>();
    for (com.sunwayMinecraft.alignments.domain.GrandAlliance alliance :
        com.sunwayMinecraft.alignments.domain.GrandAlliance.values()) {
      scores.put(alliance.getId(), getGrandAllianceScore(alliance.getId()));
    }
    return scores;
  }

  /** Placeholder for future district-ownership data; contributes 0. */
  protected int districtsHeld(String grandAllianceId) {
    return 0;
  }

  /** Placeholder for future per-alliance contract data; contributes 0. */
  protected int contractsCompleted(String grandAllianceId) {
    return 0;
  }
}
