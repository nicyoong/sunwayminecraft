package com.sunwayMinecraft.alignments.service;

import com.sunwayMinecraft.alignments.config.AlignmentConfigManager;
import com.sunwayMinecraft.alignments.domain.AlignmentDefinition;
import com.sunwayMinecraft.alignments.domain.AlignmentMembership;
import com.sunwayMinecraft.alignments.persistence.AlignmentRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Join/leave/query logic for player alignments. A player belongs to at most one
 * alignment; joining a new alignment replaces the previous membership.
 */
public class AlignmentService {
  private static final Logger LOGGER = Logger.getLogger(AlignmentService.class.getName());

  private final AlignmentConfigManager configManager;
  private final AlignmentRepository repository;

  public AlignmentService(AlignmentConfigManager configManager, AlignmentRepository repository) {
    this.configManager = configManager;
    this.repository = repository;
  }

  /** True when membership persistence is usable and changes can be applied. */
  public boolean isAvailable() {
    return repository != null && repository.isAvailable();
  }

  public AlignmentResult join(UUID playerUuid, String alignmentInput) {
    Optional<AlignmentDefinition> definition = configManager.getAlignment(alignmentInput);
    if (definition.isEmpty()) {
      return AlignmentResult.NOT_FOUND;
    }
    if (!definition.get().enabled()) {
      return AlignmentResult.DISABLED;
    }
    if (!isAvailable()) {
      return AlignmentResult.DATABASE_FAILURE;
    }

    String alignmentId = definition.get().id();
    Optional<AlignmentMembership> existing = repository.findByUuid(playerUuid);
    if (existing.isPresent() && existing.get().isSameAlignment(alignmentId)) {
      return AlignmentResult.ALREADY_ALIGNED;
    }

    AlignmentMembership membership =
        AlignmentMembership.newMembership(playerUuid, alignmentId, System.currentTimeMillis());
    if (!repository.upsert(membership)) {
      return AlignmentResult.DATABASE_FAILURE;
    }
    if (existing.isPresent()) {
      LOGGER.info(
          "Player " + playerUuid + " switched alignment to " + alignmentId);
    }
    return AlignmentResult.JOINED;
  }

  public AlignmentResult leave(UUID playerUuid) {
    if (!isAvailable()) {
      return AlignmentResult.DATABASE_FAILURE;
    }
    Optional<AlignmentMembership> existing = repository.findByUuid(playerUuid);
    if (existing.isEmpty()) {
      return AlignmentResult.NOT_ALIGNED;
    }
    if (!repository.remove(playerUuid)) {
      return AlignmentResult.DATABASE_FAILURE;
    }
    return AlignmentResult.LEFT;
  }

  public Optional<AlignmentMembership> getMembership(UUID playerUuid) {
    return repository.findByUuid(playerUuid);
  }
}
