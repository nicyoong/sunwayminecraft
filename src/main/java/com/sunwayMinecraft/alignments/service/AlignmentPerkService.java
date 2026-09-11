package com.sunwayMinecraft.alignments.service;

import com.sunwayMinecraft.alignments.config.AlignmentConfigManager;
import com.sunwayMinecraft.alignments.config.AlignmentPerksConfig;
import com.sunwayMinecraft.alignments.config.AlignmentPerksConfig.PerkDefinition;
import com.sunwayMinecraft.alignments.domain.AlignmentDefinition;
import com.sunwayMinecraft.alignments.service.AlignmentMembershipCache.CachedMembership;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Applies lightweight, rank-gated potion perks to eligible aligned players
 * on a slow repeating task and removes them when eligibility is lost.
 * The refresh only touches cached data - never the database.
 */
public class AlignmentPerkService {
  private static final Logger LOGGER = Logger.getLogger(AlignmentPerkService.class.getName());

  private final JavaPlugin plugin;
  private final AlignmentPerksConfig perksConfig;
  private final AlignmentConfigManager configManager;
  private final AlignmentRankService rankService;
  private final AlignmentMembershipCache cache;
  private final java.util.function.Predicate<Player> districtGate;
  private org.bukkit.scheduler.BukkitTask task;
  /** perk id -> applied effect type, remembered so stale perks can be removed even
   *  after they disappear from the config. */
  private final Map<UUID, Map<String, org.bukkit.potion.PotionEffectType>> activePerks = new HashMap<>();

  public AlignmentPerkService(
      JavaPlugin plugin,
      AlignmentPerksConfig perksConfig,
      AlignmentConfigManager configManager,
      AlignmentRankService rankService,
      AlignmentMembershipCache cache,
      java.util.function.Predicate<Player> districtGate) {
    this.plugin = plugin;
    this.perksConfig = perksConfig;
    this.configManager = configManager;
    this.rankService = rankService;
    this.cache = cache;
    this.districtGate = districtGate;
  }

  /** Starts the repeating refresh task. */
  public void start() {
    if (task != null) {
      task.cancel();
    }
    long intervalTicks = perksConfig.getRefreshIntervalSeconds() * 20L;
    task = Bukkit.getScheduler().runTaskTimer(plugin, this::refresh, intervalTicks, intervalTicks);
  }

  public void stop() {
    if (task != null) {
      task.cancel();
      task = null;
    }
  }

  /** One pass over all online players; safe to call from tests. */
  public void refresh() {
    if (!perksConfig.isEnabled()) {
      removeAll();
      return;
    }
    for (Player player : Bukkit.getOnlinePlayers()) {
      Optional<CachedMembership> cached = cache.getOrLoad(player.getUniqueId());
      Optional<AlignmentDefinition> definition = cached.flatMap(
          membership -> configManager.getAlignment(membership.alignmentId()));
      reconcile(player, cached.orElse(null), definition.orElse(null));
    }
  }

  /**
   * Applies newly eligible perks and removes ones the player no longer
   * qualifies for. Public for testing.
   */
  public void reconcile(Player player, CachedMembership cached, AlignmentDefinition definition) {
    UUID playerUuid = player.getUniqueId();
    Map<String, org.bukkit.potion.PotionEffectType> active =
        activePerks.getOrDefault(playerUuid, new HashMap<>());

    Set<String> desired = new HashSet<>();
    if (cached != null && definition != null && definition.enabled()
        && "active".equals(cached.status())) {
      for (PerkDefinition perk : perksConfig.getPerks().values()) {
        if (isEligible(player, perk, cached, definition)) {
          desired.add(perk.id());
        }
      }
    }

    // drop everything active that is no longer desired, even if the perk
    // vanished from the config entirely
    for (String perkId : new HashSet<>(active.keySet())) {
      if (desired.contains(perkId)) {
        continue;
      }
      org.bukkit.potion.PotionEffectType remembered = active.get(perkId);
      if (remembered != null) {
        player.removePotionEffect(remembered);
      }
      active.remove(perkId);
    }
    // apply or re-apply desired perks
    for (PerkDefinition perk : perksConfig.getPerks().values()) {
      if (!desired.contains(perk.id())) {
        continue;
      }
      if (!active.containsKey(perk.id())) {
        apply(player, perk);
        active.put(perk.id(), perk.effect());
      }
    }

    if (active.isEmpty()) {
      activePerks.remove(playerUuid);
    } else {
      activePerks.put(playerUuid, active);
    }
  }

  private boolean isEligible(Player player, PerkDefinition perk, CachedMembership cached,
      AlignmentDefinition definition) {
    if (!perk.enabled()) {
      return false;
    }
    if (player.hasPermission("sunway.align.perks.admin")) {
      return true; // testing bypass: rank and district gates ignored
    }
    if (!rankService.meetsRank(
        cached.reputation(), definition.grandAlliance().getId(), perk.minimumRank())) {
      return false;
    }
    if (!perksConfig.isApplyInAnyDistrict()) {
      return isInQualifyingDistrict(player);
    }
    return true;
  }

  private boolean isInQualifyingDistrict(Player player) {
    return districtGate != null && districtGate.test(player);
  }

  private void apply(Player player, PerkDefinition perk) {
    int durationTicks = perksConfig.getDurationSeconds() * 20;
    player.addPotionEffect(
        new PotionEffect(perk.effect(), durationTicks, amplifierFor(perk), true, false, true));
  }

  private int amplifierFor(PerkDefinition perk) {
    return Math.min(perk.amplifier(), perksConfig.getMaxAmplifier());
  }

  private void removeAll() {
    for (UUID playerUuid : new HashSet<>(activePerks.keySet())) {
      Player player = Bukkit.getServer() == null ? null : Bukkit.getPlayer(playerUuid);
      if (player != null) {
        for (org.bukkit.potion.PotionEffectType effect : activePerks.get(playerUuid).values()) {
          player.removePotionEffect(effect);
        }
      }
      activePerks.remove(playerUuid);
    }
  }

  /** Perk ids currently applied to the player; for tests and debugging. */
  public Set<String> getActivePerkIds(UUID playerUuid) {
    return new HashSet<>(activePerks.getOrDefault(playerUuid, Map.of()).keySet());
  }

  /**
   * Effective switch cooldown for the player, applying the configured
   * cooldown_reduction perk when their rank qualifies.
   */
  public long applyCooldownReduction(UUID playerUuid, long baseCooldownSeconds) {
    AlignmentPerksConfig.CooldownReduction reduction = perksConfig.getCooldownReduction();
    if (!perksConfig.isEnabled() || !reduction.enabled() || baseCooldownSeconds <= 0) {
      return baseCooldownSeconds;
    }
    Optional<CachedMembership> cached = cache.getOrLoad(playerUuid);
    if (cached.isEmpty()) {
      return baseCooldownSeconds;
    }
    if (!rankService.meetsRank(
        cached.get().reputation(), cached.get().grandAllianceId(), reduction.minimumRank())) {
      return baseCooldownSeconds;
    }
    return baseCooldownSeconds * (100 - reduction.percent()) / 100;
  }

  /** Drops tracked perks for a player (called on alignment changes). */
  public void forgetPlayer(UUID playerUuid) {
    Player player = Bukkit.getPlayer(playerUuid);
    if (player != null) {
      for (org.bukkit.potion.PotionEffectType effect : activePerks.get(playerUuid).values()) {
        player.removePotionEffect(effect);
      }
    }
    activePerks.remove(playerUuid);
  }
}
