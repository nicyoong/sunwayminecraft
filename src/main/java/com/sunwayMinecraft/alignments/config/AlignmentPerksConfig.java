package com.sunwayMinecraft.alignments.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Lightweight alignment perks, loaded from alignment-perks.yml. Only four
 * benign effect types are supported; unknown or dangerous entries are
 * skipped with a logged warning.
 */
public class AlignmentPerksConfig {
  /** A single perk: a potion effect unlocked at a minimum rank. */
  public record PerkDefinition(
      String id, String displayName, PotionEffectType effect, String minimumRank,
      int amplifier, boolean enabled) {}

  /** Reduced alignment switch cooldown for higher ranks. */
  public record CooldownReduction(boolean enabled, String minimumRank, int percent) {}

  private final JavaPlugin plugin;
  private final File configFile;

  private boolean enabled = true;
  private int refreshIntervalSeconds = 5;
  private int durationSeconds = 15;
  private int maxAmplifier = 1;
  private boolean applyInAnyDistrict = true;
  private boolean applyInDisabledDistricts = false;
  private final Map<String, PerkDefinition> perks = new HashMap<>();
  private CooldownReduction cooldownReduction = new CooldownReduction(true, "steward", 25);

  public AlignmentPerksConfig(JavaPlugin plugin) {
    this.plugin = plugin;
    this.configFile = new File(plugin.getDataFolder(), "alignment-perks.yml");
    if (!configFile.exists()) {
      plugin.saveResource("alignment-perks.yml", false);
    }
  }

  public void load() {
    perks.clear();
    YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
    enabled = config.getBoolean("enabled", true);
    refreshIntervalSeconds = Math.max(1, config.getInt("refresh_interval_seconds", 5));
    durationSeconds = Math.max(1, config.getInt("duration_seconds", 15));
    maxAmplifier = Math.max(0, config.getInt("max_amplifier", 1));
    applyInAnyDistrict = config.getBoolean("apply_in_any_district", true);
    applyInDisabledDistricts = config.getBoolean("apply_in_disabled_districts", false);
    loadPerks(config.getConfigurationSection("perks"));
    loadCooldownReduction(config.getConfigurationSection("cooldown_reduction"));
  }

  private void loadPerks(ConfigurationSection section) {
    if (section == null) {
      plugin.getLogger().warning("alignment-perks.yml is missing the perks section");
      return;
    }
    for (String key : section.getKeys(false)) {
      String effectKey = section.getString(key + ".effect", "");
      Optional<PotionEffectType> effect = resolveEffect(effectKey);
      if (effect.isEmpty()) {
        plugin.getLogger().warning("Perk " + key + " has unsupported effect '" + effectKey
            + "' (supported: speed, haste, regeneration, saturation); skipped");
        continue;
      }
      String displayName = section.getString(key + ".display_name", key);
      perks.put(key, new PerkDefinition(
          key,
          displayName,
          effect.get(),
          section.getString(key + ".minimum_rank", "initiate").toLowerCase(Locale.ROOT),
          Math.max(0, section.getInt(key + ".amplifier", 0)),
          section.getBoolean(key + ".enabled", true)));
    }
  }

  private void loadCooldownReduction(ConfigurationSection section) {
    if (section == null) return;
    cooldownReduction = new CooldownReduction(
        section.getBoolean("enabled", true),
        section.getString("minimum_rank", "steward").toLowerCase(Locale.ROOT),
        Math.min(90, Math.max(0, section.getInt("percent", 25))));
  }

  /** Maps the four supported effect keys to Bukkit potion effect types. */
  private Optional<PotionEffectType> resolveEffect(String key) {
    if (key == null) return Optional.empty();
    return switch (key.trim().toLowerCase(Locale.ROOT)) {
      case "speed" -> Optional.of(PotionEffectType.SPEED);
      case "haste" -> Optional.of(PotionEffectType.HASTE);
      case "regeneration" -> Optional.of(PotionEffectType.REGENERATION);
      case "saturation" -> Optional.of(PotionEffectType.SATURATION);
      default -> Optional.empty();
    };
  }

  public boolean isEnabled() {
    return enabled;
  }

  public int getRefreshIntervalSeconds() {
    return refreshIntervalSeconds;
  }

  public int getDurationSeconds() {
    return durationSeconds;
  }

  public int getMaxAmplifier() {
    return maxAmplifier;
  }

  public boolean isApplyInAnyDistrict() {
    return applyInAnyDistrict;
  }

  public boolean isApplyInDisabledDistricts() {
    return applyInDisabledDistricts;
  }

  public Map<String, PerkDefinition> getPerks() {
    return perks;
  }

  public CooldownReduction getCooldownReduction() {
    return cooldownReduction;
  }
}
