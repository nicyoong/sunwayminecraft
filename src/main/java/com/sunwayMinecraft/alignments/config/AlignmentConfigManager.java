package com.sunwayMinecraft.alignments.config;

import com.sunwayMinecraft.alignments.domain.AlignmentDefinition;
import com.sunwayMinecraft.alignments.domain.Campus;
import com.sunwayMinecraft.alignments.domain.GrandAlliance;
import com.sunwayMinecraft.alignments.domain.GrandAllianceDefinition;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Loads and validates alignments.yml. Definitions whose ids differ only by case
 * are treated as duplicates; entries referencing unknown grand alliances or
 * campuses are skipped with a logged warning.
 */
public class AlignmentConfigManager {
  private final JavaPlugin plugin;
  private final File configFile;
  private final Map<String, GrandAllianceDefinition> alliances = new HashMap<>();
  private final Map<String, AlignmentDefinition> alignments = new HashMap<>();
  private boolean loaded = false;

  public AlignmentConfigManager(JavaPlugin plugin) {
    this.plugin = plugin;
    this.configFile = new File(plugin.getDataFolder(), "alignments.yml");
    if (!configFile.exists()) {
      plugin.saveResource("alignments.yml", false);
    }
  }

  public void load() {
    alliances.clear();
    alignments.clear();

    YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
    loadAlliances(config.getConfigurationSection("grand_alliances"));
    loadAlignments(config.getConfigurationSection("alignments"));
    loaded = true;

    if (alliances.isEmpty()) {
      plugin.getLogger().severe("No valid grand alliances found in alignments.yml");
    }
    if (alignments.isEmpty()) {
      plugin.getLogger().severe("No valid alignments found in alignments.yml");
    } else {
      plugin.getLogger()
          .info(
              "Loaded "
                  + alignments.size()
                  + " alignments across "
                  + alliances.size()
                  + " grand alliances");
    }
  }

  private void loadAlliances(ConfigurationSection section) {
    if (section == null) {
      plugin.getLogger().warning("alignments.yml is missing the grand_alliances section");
      return;
    }
    for (String key : section.getKeys(false)) {
      Optional<GrandAlliance> alliance = GrandAlliance.fromId(key);
      if (alliance.isEmpty()) {
        plugin.getLogger().warning("Unknown grand alliance id in alignments.yml: " + key);
        continue;
      }
      String displayName = section.getString(key + ".display_name");
      if (displayName == null || displayName.isBlank()) {
        plugin.getLogger().warning("Grand alliance " + key + " is missing display_name; skipped");
        continue;
      }
      alliances.put(
          key,
          new GrandAllianceDefinition(
              key,
              displayName,
              section.getString(key + ".description", ""),
              section.getString(key + ".chat_color", "&f")));
    }
  }

  private void loadAlignments(ConfigurationSection section) {
    if (section == null) {
      plugin.getLogger().warning("alignments.yml is missing the alignments section");
      return;
    }
    for (String rawKey : section.getKeys(false)) {
      String key = rawKey.toLowerCase(Locale.ROOT);
      if (!key.equals(rawKey)) {
        plugin.getLogger()
            .warning(
                "Alignment id '" + rawKey + "' should be lowercase; treating it as '" + key + "'");
      }
      if (alignments.containsKey(key) || alliances.containsKey(key)) {
        plugin.getLogger().warning("Duplicate alignment id in alignments.yml: " + key);
        continue;
      }
      try {
        AlignmentDefinition definition = parseAlignment(key, section.getConfigurationSection(rawKey));
        if (definition != null) {
          alignments.put(key, definition);
        }
      } catch (Exception e) {
        plugin.getLogger().log(Level.SEVERE, "Failed to load alignment: " + key, e);
      }
    }
  }

  private AlignmentDefinition parseAlignment(String key, ConfigurationSection section) {
    if (section == null) {
      plugin.getLogger().warning("Alignment " + key + " has no configuration section; skipped");
      return null;
    }
    String displayName = section.getString("display_name");
    if (displayName == null || displayName.isBlank()) {
      plugin.getLogger().warning("Alignment " + key + " is missing display_name; skipped");
      return null;
    }

    String allianceId = section.getString("grand_alliance");
    Optional<GrandAlliance> alliance = GrandAlliance.fromId(allianceId);
    if (alliance.isEmpty()) {
      plugin.getLogger()
          .warning(
              "Alignment " + key + " references unknown or missing grand_alliance '" + allianceId
                  + "'; skipped");
      return null;
    }

    String campusId = section.getString("home_campus");
    Optional<Campus> campus = Campus.fromId(campusId);
    if (campus.isEmpty()) {
      plugin.getLogger()
          .warning(
              "Alignment " + key + " references unknown or missing home_campus '" + campusId
                  + "'; skipped");
      return null;
    }

    String chatPrefix = section.getString("chat_prefix");
    if (chatPrefix == null || chatPrefix.isBlank()) {
      plugin.getLogger()
          .warning("Alignment " + key + " has no chat_prefix; defaulting to its display name");
      chatPrefix = "[" + displayName + "]";
    }

    boolean enabled = section.getBoolean("enabled", true);

    return new AlignmentDefinition(
        key,
        displayName,
        alliance.get(),
        campus.get(),
        section.getString("description", ""),
        chatPrefix,
        section.getString("chat_color", "&f"),
        enabled);
  }

  public boolean isLoaded() {
    return loaded;
  }

  public Collection<GrandAllianceDefinition> getAllianceDefinitions() {
    return alliances.values();
  }

  public Optional<GrandAllianceDefinition> getAllianceDefinition(GrandAlliance alliance) {
    return Optional.ofNullable(alliances.get(alliance.getId()));
  }

  public Collection<AlignmentDefinition> getAllAlignments() {
    return alignments.values();
  }

  public Optional<AlignmentDefinition> getAlignment(String id) {
    if (id == null) return Optional.empty();
    return Optional.ofNullable(alignments.get(id.trim().toLowerCase(Locale.ROOT)));
  }

  public List<AlignmentDefinition> getAlignmentsByGrandAlliance(GrandAlliance alliance) {
    List<AlignmentDefinition> result = new ArrayList<>();
    for (AlignmentDefinition definition : alignments.values()) {
      if (definition.grandAlliance() == alliance) {
        result.add(definition);
      }
    }
    return result;
  }

  public List<AlignmentDefinition> getAlignmentsByCampus(Campus campus) {
    List<AlignmentDefinition> result = new ArrayList<>();
    for (AlignmentDefinition definition : alignments.values()) {
      if (definition.homeCampus() == campus) {
        result.add(definition);
      }
    }
    return result;
  }

  public List<AlignmentDefinition> getEnabledAlignments() {
    List<AlignmentDefinition> result = new ArrayList<>();
    for (AlignmentDefinition definition : alignments.values()) {
      if (definition.enabled()) {
        result.add(definition);
      }
    }
    return result;
  }
}
