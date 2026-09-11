package com.sunwayMinecraft.alignments.service;

import com.sunwayMinecraft.alignments.config.AlignmentConfigManager;
import com.sunwayMinecraft.alignments.config.AlignmentSettingsConfig;
import com.sunwayMinecraft.alignments.domain.AlignmentDefinition;
import com.sunwayMinecraft.alignments.domain.AlignmentMembership;
import com.sunwayMinecraft.alignments.persistence.AlignmentRepository;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * In-memory view of player memberships for online players. Chat formatting
 * and commands read from this cache first; misses fall through to SQLite.
 * Entries are loaded on join and dropped on quit.
 */
public class AlignmentMembershipCache {
  private static final Logger LOGGER = Logger.getLogger(AlignmentMembershipCache.class.getName());
  private static final long DB_UNAVAILABLE_WARNING_INTERVAL_MS = 30_000;

  /** Immutable snapshot of a player's alignment for fast chat/lookup paths. */
  public record CachedMembership(
      UUID playerUuid,
      String alignmentId,
      String grandAllianceId,
      String campusId,
      int reputation,
      String status,
      long lastUpdated) {}

  private final AlignmentConfigManager configManager;
  private final AlignmentRepository repository;
  private final Map<UUID, CachedMembership> cache = new ConcurrentHashMap<>();
  private long lastDbUnavailableWarning = 0;

  public AlignmentMembershipCache(AlignmentConfigManager configManager, AlignmentRepository repository) {
    this.configManager = configManager;
    this.repository = repository;
  }

  /** Populates the cache for everyone online; used on plugin enable. */
  public void loadOnlinePlayers() {
    for (Player player : Bukkit.getOnlinePlayers()) {
      load(player.getUniqueId());
    }
  }

  /** Loads the player's membership from SQLite into the cache if known. */
  public void load(UUID playerUuid) {
    Optional<CachedMembership> loaded = loadFromRepository(playerUuid);
    loaded.ifPresent(membership -> cache.put(playerUuid, membership));
  }

  public Optional<CachedMembership> get(UUID playerUuid) {
    return Optional.ofNullable(cache.get(playerUuid));
  }

  /**
   * Cache-first lookup. On a miss the membership is read from SQLite; if the
   * database is unavailable the last known cached value wins and a warning
   * is logged (rate limited so chat cannot spam the console).
   */
  public Optional<CachedMembership> getOrLoad(UUID playerUuid) {
    CachedMembership cached = cache.get(playerUuid);
    if (cached != null) {
      return Optional.of(cached);
    }
    Optional<CachedMembership> loaded = loadFromRepository(playerUuid);
    if (loaded.isPresent()) {
      cache.put(playerUuid, loaded.get());
      return loaded;
    }
    if (!repository.isAvailable() && cached == null) {
      warnDbUnavailable();
    }
    return Optional.empty();
  }

  public void update(UUID playerUuid, AlignmentMembership membership, AlignmentDefinition definition) {
    cache.put(
        playerUuid,
        new CachedMembership(
            playerUuid,
            definition.id(),
            definition.grandAlliance().getId(),
            definition.homeCampus().getId(),
            membership.reputation(),
            membership.status(),
            System.currentTimeMillis()));
  }

  public void updateStatus(UUID playerUuid, String status) {
    CachedMembership cached = cache.get(playerUuid);
    if (cached == null) return;
    cache.put(
        playerUuid,
        new CachedMembership(
            cached.playerUuid(),
            cached.alignmentId(),
            cached.grandAllianceId(),
            cached.campusId(),
            cached.reputation(),
            status,
            System.currentTimeMillis()));
  }

  public void invalidate(UUID playerUuid) {
    cache.remove(playerUuid);
  }

  public int size() {
    return cache.size();
  }

  /**
   * Re-applies config after /align reload: cached players whose alignment
   * vanished or is disabled are either marked inactive (KEEP) or unaligned
   * (UNALIGN), based on stale_membership_action.
   */
  public void revalidate(AlignmentSettingsConfig settings) {
    int changed = 0;
    for (CachedMembership cached : cache.values()) {
      Optional<AlignmentDefinition> definition = configManager.getAlignment(cached.alignmentId());
      if (definition.isPresent() && definition.get().enabled()) {
        continue;
      }
      changed++;
      UUID playerUuid = cached.playerUuid();
      if (settings.getStaleMembershipAction() == AlignmentSettingsConfig.StaleMembershipAction.UNALIGN) {
        repository.remove(playerUuid);
        cache.remove(playerUuid);
        LOGGER.info("Alignment of player " + playerUuid + " no longer exists; membership removed");
      } else {
        updateStatus(playerUuid, "inactive");
        LOGGER.warning(
            "Alignment " + cached.alignmentId() + " of player " + playerUuid
                + " is no longer available; membership marked inactive");
      }
    }
    if (changed > 0) {
      LOGGER.warning("Revalidated alignment cache after reload: " + changed + " stale memberships");
    }
  }

  private Optional<CachedMembership> loadFromRepository(UUID playerUuid) {
    Optional<AlignmentMembership> membership = repository.findByUuid(playerUuid);
    if (membership.isEmpty()) {
      return Optional.empty();
    }
    Optional<AlignmentDefinition> definition = configManager.getAlignment(membership.get().alignmentId());
    if (definition.isEmpty()) {
      LOGGER.warning(
          "Cached membership of player " + playerUuid + " references unknown alignment "
              + membership.get().alignmentId());
      return Optional.empty();
    }
    AlignmentDefinition def = definition.get();
    return Optional.of(
        new CachedMembership(
            playerUuid,
            def.id(),
            def.grandAlliance().getId(),
            def.homeCampus().getId(),
            membership.get().reputation(),
            membership.get().status(),
            System.currentTimeMillis()));
  }

  private void warnDbUnavailable() {
    long now = System.currentTimeMillis();
    if (now - lastDbUnavailableWarning >= DB_UNAVAILABLE_WARNING_INTERVAL_MS) {
      lastDbUnavailableWarning = now;
      LOGGER.warning("Alignment database unavailable; serving last known cached memberships");
    }
  }
}
