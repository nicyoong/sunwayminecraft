package com.sunwayMinecraft.alignments.service;

import com.sunwayMinecraft.alignments.config.AlignmentConfigManager;
import com.sunwayMinecraft.alignments.config.AlignmentSettingsConfig;
import com.sunwayMinecraft.alignments.domain.AlignmentDefinition;
import com.sunwayMinecraft.alignments.domain.AlignmentMembership;
import com.sunwayMinecraft.alignments.persistence.AlignmentRepository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Join/leave/query logic for player alignments. A player belongs to at most
 * one alignment; joining a new alignment replaces the previous membership,
 * records a switch cooldown, and refreshes the membership cache.
 */
public class AlignmentService {
  private static final Logger LOGGER = Logger.getLogger(AlignmentService.class.getName());

  private final AlignmentConfigManager configManager;
  private final AlignmentSettingsConfig settings;
  private final AlignmentRepository repository;
  private final AlignmentCooldownManager cooldownManager;
  private final AlignmentMembershipCache cache;

  public AlignmentService(
      AlignmentConfigManager configManager,
      AlignmentSettingsConfig settings,
      AlignmentRepository repository,
      AlignmentCooldownManager cooldownManager,
      AlignmentMembershipCache cache) {
    this.configManager = configManager;
    this.settings = settings;
    this.repository = repository;
    this.cooldownManager = cooldownManager;
    this.cache = cache;
  }

  /** True when membership persistence is usable and changes can be applied. */
  public boolean isAvailable() {
    return repository != null && repository.isAvailable();
  }

  public AlignmentResult join(UUID playerUuid, String alignmentInput) {
    return join(playerUuid, alignmentInput, false);
  }

  public AlignmentResult join(UUID playerUuid, String alignmentInput, boolean bypassCooldown) {
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
    if (!bypassCooldown) {
      long remaining =
          cooldownManager.getRemainingSeconds(playerUuid, settings.getSwitchCooldownSeconds());
      if (remaining > 0) {
        return AlignmentResult.COOLDOWN_ACTIVE;
      }
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
    cooldownManager.recordSwitch(playerUuid);
    cache.update(playerUuid, membership, definition.get());
    if (existing.isPresent()) {
      LOGGER.info("Player " + playerUuid + " switched alignment to " + alignmentId);
    } else {
      LOGGER.info("Player " + playerUuid + " joined alignment " + alignmentId);
    }
    return AlignmentResult.JOINED;
  }

  public AlignmentResult leave(UUID playerUuid) {
    return leave(playerUuid, settings.isCooldownAppliesToLeave());
  }

  public AlignmentResult leave(UUID playerUuid, boolean recordCooldown) {
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
    cache.invalidate(playerUuid);
    if (recordCooldown) {
      cooldownManager.recordSwitch(playerUuid);
    }
    LOGGER.info("Player " + playerUuid + " left alignment " + existing.get().alignmentId());
    return AlignmentResult.LEFT;
  }

  /**
   * Forces a player into an alignment (admin action): bypasses the cooldown
   * and the already-aligned check, updates database and cache.
   */
  public AlignmentResult adminSet(UUID targetUuid, String alignmentInput) {
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
    AlignmentMembership membership =
        AlignmentMembership.newMembership(
            targetUuid, definition.get().id(), System.currentTimeMillis());
    if (!repository.upsert(membership)) {
      return AlignmentResult.DATABASE_FAILURE;
    }
    cache.update(targetUuid, membership, definition.get());
    LOGGER.info("[admin] Player " + targetUuid + " was set to alignment " + definition.get().id());
    return AlignmentResult.JOINED;
  }

  /** Removes a player's membership (admin action); reports if unaligned. */
  public AlignmentResult adminClear(UUID targetUuid) {
    if (!isAvailable()) {
      return AlignmentResult.DATABASE_FAILURE;
    }
    Optional<AlignmentMembership> existing = repository.findByUuid(targetUuid);
    if (existing.isEmpty()) {
      return AlignmentResult.NOT_ALIGNED;
    }
    if (!repository.remove(targetUuid)) {
      return AlignmentResult.DATABASE_FAILURE;
    }
    cache.invalidate(targetUuid);
    LOGGER.info("[admin] Player " + targetUuid + " was cleared from alignment "
        + existing.get().alignmentId());
    return AlignmentResult.LEFT;
  }

  /** Seconds until the player may join an alignment again; 0 when free. */
  public long getRemainingCooldownSeconds(UUID playerUuid) {
    return cooldownManager.getRemainingSeconds(playerUuid, settings.getSwitchCooldownSeconds());
  }

  public AlignmentCooldownManager getCooldownManager() {
    return cooldownManager;
  }

  /** Member count per alignment id across database, empty on failure. */
  public Map<String, Integer> getAlignmentMemberCounts() {
    return repository.countByAlignment();
  }

  public Optional<AlignmentMembership> getMembership(UUID playerUuid) {
    return repository.findByUuid(playerUuid);
  }

  public AlignmentMembershipCache getCache() {
    return cache;
  }
}
