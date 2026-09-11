package com.sunwayMinecraft.districts.service;

import com.sunwayMinecraft.districts.config.DistrictsConfigManager;
import com.sunwayMinecraft.districts.domain.DistrictAccessRule;
import com.sunwayMinecraft.districts.domain.DistrictDefinition;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Alignment-aware access decisions for districts. Part 1 keeps these as
 * reusable read-only queries: nothing here is wired into block, residency
 * or economy enforcement yet - callers opt in.
 *
 * <p>The player's alignment comes from an injected resolver so this service
 * works independently of the alignment feature; without a resolver every
 * player is treated as unaligned.
 */
public class DistrictAlignmentService {
  public static final String ADMIN_BYPASS_PERMISSION = "sunway.district.admin.bypass";

  private final DistrictsConfigManager configManager;
  private final Function<UUID, Optional<String>> playerAlignmentResolver;

  public DistrictAlignmentService(
      DistrictsConfigManager configManager,
      Function<UUID, Optional<String>> playerAlignmentResolver) {
    this.configManager = configManager;
    this.playerAlignmentResolver = playerAlignmentResolver;
  }

  /** Whether the given alignment may access the district at all. */
  public boolean canAlignmentAccessDistrict(String alignmentId, DistrictDefinition district) {
    return accessRule(district).allows(alignmentId);
  }

  /** Whether the player's alignment grants access (admins always pass). */
  public boolean canPlayerAccessDistrict(Player player, DistrictDefinition district) {
    if (hasBypass(player)) {
      return true;
    }
    return accessFor(player, district);
  }

  /**
   * Building: denied without access, in disabled districts, and in archived
   * districts (read-only heritage) unless the admin bypass applies.
   */
  public boolean canPlayerBuildInDistrict(Player player, DistrictDefinition district) {
    if (hasBypass(player)) {
      return true;
    }
    if (district == null || !district.isEnabled() || district.isArchived()) {
      return false;
    }
    return accessFor(player, district);
  }

  /** Renting: denied without access and in archived districts. */
  public boolean canPlayerRentInDistrict(Player player, DistrictDefinition district) {
    if (hasBypass(player)) {
      return true;
    }
    if (district == null || district.isArchived()) {
      return false;
    }
    return accessFor(player, district);
  }

  /** Trading: denied without access and in archived districts. */
  public boolean canPlayerTradeInDistrict(Player player, DistrictDefinition district) {
    if (hasBypass(player)) {
      return true;
    }
    if (district == null || district.isArchived()) {
      return false;
    }
    return accessFor(player, district);
  }

  /**
   * Property policy id for a district: the configured reference when it
   * exists in property-policies.yml, otherwise the default policy.
   */
  public String resolvePropertyPolicyId(DistrictDefinition district) {
    return configManager.resolvePropertyPolicyId(district);
  }

  private DistrictAccessRule accessRule(DistrictDefinition district) {
    return DistrictAccessRule.forOwnership(district == null ? null : district.getOwnership());
  }

  private boolean accessFor(Player player, DistrictDefinition district) {
    DistrictAccessRule rule = accessRule(district);
    Optional<String> alignment = playerAlignmentResolver.apply(player.getUniqueId());
    return alignment.map(rule::allows).orElseGet(rule::allowsUnaligned);
  }

  private boolean hasBypass(Player player) {
    return player != null && player.hasPermission(ADMIN_BYPASS_PERMISSION);
  }
}
