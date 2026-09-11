package com.sunwayMinecraft.alignments.service;

import com.sunwayMinecraft.alignments.persistence.AlignmentRepository;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Tracks when players last joined/switched alignment so /align join can be
 * rate limited. The in-memory map is backed by SQLite so cooldowns survive
 * restarts; admin-driven /align set never records a switch.
 */
public class AlignmentCooldownManager {
  private static final Logger LOGGER = Logger.getLogger(AlignmentCooldownManager.class.getName());

  private final AlignmentRepository repository;
  private final Map<UUID, Long> lastSwitchAt = new ConcurrentHashMap<>();

  public AlignmentCooldownManager(AlignmentRepository repository) {
    this.repository = repository;
  }

  /**
   * Seconds left before the player may join again, or 0 when no cooldown is
   * active. Cooldown window is supplied by the caller from settings so a
   * config change applies immediately.
   */
  public long getRemainingSeconds(UUID playerUuid, long cooldownSeconds) {
    if (cooldownSeconds <= 0) {
      return 0;
    }
    Long last = lastSwitchAt.get(playerUuid);
    if (last == null) {
      last = repository.getLastSwitchAt(playerUuid).orElse(0L);
      if (last > 0) {
        lastSwitchAt.put(playerUuid, last);
      }
    }
    if (last <= 0) {
      return 0;
    }
    long elapsed = (System.currentTimeMillis() - last) / 1000;
    return Math.max(0, cooldownSeconds - elapsed);
  }

  /** Persists a switch timestamp in memory and SQLite. */
  public void recordSwitch(UUID playerUuid) {
    long now = System.currentTimeMillis();
    lastSwitchAt.put(playerUuid, now);
    if (!repository.setLastSwitchAt(playerUuid, now)) {
      LOGGER.warning(
          "Could not persist alignment switch cooldown for " + playerUuid
              + "; the in-memory cooldown still applies until restart");
    }
  }
}
