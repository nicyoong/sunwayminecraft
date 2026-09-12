package com.sunwayMinecraft.contracts.config;

import com.sunwayMinecraft.alignments.domain.Campus;
import com.sunwayMinecraft.contracts.domain.ContractAlignmentRule;
import com.sunwayMinecraft.contracts.domain.ContractCampusRoute;
import com.sunwayMinecraft.contracts.domain.ContractCategory;
import com.sunwayMinecraft.contracts.domain.ContractDefinition;
import com.sunwayMinecraft.contracts.domain.ContractObjectiveType;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;

/**
 * Loads contracts.yml into alignment-aware definitions. Entries referencing
 * unknown alignments or campuses are disabled with a warning instead of
 * crashing the plugin; {@link #getContracts()} only ever returns usable ones.
 */
public class ContractConfigManager {
    private final JavaPlugin plugin;
    private final File configFile;
    private final Map<String, ContractDefinition> contracts = new HashMap<>();
    private final Map<String, ContractDefinition> disabledContracts = new LinkedHashMap<>();
    private final Map<String, String> disabledReasons = new LinkedHashMap<>();
    private Predicate<String> alignmentIdValidator;
    private Predicate<String> campusIdValidator;

    public ContractConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "contracts.yml");
        if (!configFile.exists()) {
            plugin.saveResource("contracts.yml", false);
        }
        this.alignmentIdValidator = defaultAlignmentIdValidator();
        this.campusIdValidator = defaultCampusIdValidator();
    }

    public void load() {
        contracts.clear();
        disabledContracts.clear();
        disabledReasons.clear();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        ConfigurationSection section = config.getConfigurationSection("contracts");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            try {
                ContractDefinition definition = parseContract(key, section);
                if (definition.enabled()) {
                    contracts.put(key, definition);
                } else {
                    disabledReasons.put(key, "disabled in contracts.yml");
                    plugin.getLogger().warning("Contract '" + key + "' disabled: disabled in contracts.yml");
                }
            } catch (ContractDisabledException e) {
                disabledReasons.put(key, e.getMessage());
                plugin.getLogger().warning("Contract '" + key + "' disabled: " + e.getMessage());
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load contract: " + key, e);
            }
        }
    }

    private ContractDefinition parseContract(String key, ConfigurationSection section) {
        String typeValue = section.getString(key + ".contract_type",
                section.getString(key + ".category"));
        ContractCategory category;
        try {
            category = ContractCategory.valueOf(typeValue.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ContractDisabledException("unknown contract type: " + typeValue);
        }
        String name = section.getString(key + ".display_name", section.getString(key + ".name"));
        String description = section.getString(key + ".description");
        double reward = section.getDouble(key + ".reward_money");
        long reputation = section.getLong(key + ".reward_reputation", 0);
        long duration = section.getLong(key + ".duration_minutes", 60);
        long cooldown = section.getLong(key + ".cooldown_minutes", 30);
        String startEndpoint = section.getString(key + ".start_endpoint");
        String endEndpoint = section.getString(key + ".end_endpoint");
        String objective = section.getString(key + ".objective_description");
        String objectiveTypeValue = section.getString(key + ".objective_type");
        ContractObjectiveType objectiveType = objectiveTypeValue == null
            ? ContractDefinition.defaultObjectiveType(category)
            : ContractObjectiveType.valueOf(objectiveTypeValue.toUpperCase(Locale.ROOT));

        Map<Material, Integer> materials = new HashMap<>();
        ConfigurationSection matSection = section.getConfigurationSection(key + ".required_materials");
        if (matSection != null) {
            for (String matKey : matSection.getKeys(false)) {
                Material material = Material.matchMaterial(matKey);
                if (material != null) {
                    materials.put(material, matSection.getInt(matKey));
                }
            }
        }

        ContractAlignmentRule alignmentRule = parseAlignmentRule(key, section);
        ContractCampusRoute campusRoute = new ContractCampusRoute(
                lowercase(section.getString(key + ".origin_campus")),
                lowercase(section.getString(key + ".destination_campus")),
                lowercase(section.getString(key + ".origin_alignment")),
                lowercase(section.getString(key + ".destination_alignment")));

        validateAlignmentIds(key, alignmentRule, campusRoute);
        validateCampusIds(key, campusRoute);

        boolean enabled = section.getBoolean(key + ".enabled", true);
        return new ContractDefinition(
            key, category, name, description, reward, duration, cooldown,
            startEndpoint, endEndpoint, materials, objective, objectiveType,
            alignmentRule, campusRoute, reputation, enabled
        );
    }

    private ContractAlignmentRule parseAlignmentRule(String key, ConfigurationSection section) {
        String required = lowercase(section.getString(key + ".required_alignment"));
        String recommended = lowercase(section.getString(key + ".recommended_alignment"));
        List<String> forbidden = new ArrayList<>();
        if (section.isList(key + ".forbidden_alignment")) {
            for (String value : section.getStringList(key + ".forbidden_alignment")) {
                forbidden.add(value.toLowerCase(Locale.ROOT));
            }
        } else if (section.getString(key + ".forbidden_alignment") != null) {
            forbidden.add(section.getString(key + ".forbidden_alignment").toLowerCase(Locale.ROOT));
        }
        return new ContractAlignmentRule(required, recommended, forbidden);
    }

    private void validateAlignmentIds(String key, ContractAlignmentRule rule,
                                      ContractCampusRoute route) {
        List<String> referenced = new ArrayList<>();
        if (rule.requiredAlignment() != null) referenced.add(rule.requiredAlignment());
        if (rule.recommendedAlignment() != null) referenced.add(rule.recommendedAlignment());
        if (route.originAlignment() != null) referenced.add(route.originAlignment());
        if (route.destinationAlignment() != null) referenced.add(route.destinationAlignment());
        referenced.addAll(rule.forbiddenAlignments());
        for (String alignmentId : referenced) {
            if (!alignmentIdValidator.test(alignmentId)) {
                throw new ContractDisabledException("unknown alignment id: " + alignmentId);
            }
        }
    }

    private void validateCampusIds(String key, ContractCampusRoute route) {
        List<String> referenced = new ArrayList<>();
        if (route.originCampus() != null) referenced.add(route.originCampus());
        if (route.destinationCampus() != null) referenced.add(route.destinationCampus());
        for (String campusId : referenced) {
            if (!campusIdValidator.test(campusId)) {
                throw new ContractDisabledException("unknown campus id: " + campusId);
            }
        }
    }

    /** Moves a loaded contract out of the usable pool, recording why for /contracts admin list. */
    public void disableContract(String id, String reason) {
        ContractDefinition definition = contracts.remove(id);
        if (definition != null) {
            disabledContracts.put(id, definition);
        }
        disabledReasons.put(id, reason);
        plugin.getLogger().warning("Contract '" + id + "' disabled: " + reason);
    }

    /** Overrides the default alignments.yml-derived alignment id validation. */
    public void setAlignmentIdValidator(Predicate<String> alignmentIdValidator) {
        this.alignmentIdValidator = alignmentIdValidator != null
                ? alignmentIdValidator
                : defaultAlignmentIdValidator();
    }

    /** Overrides the default Campus enum-derived campus id validation. */
    public void setCampusIdValidator(Predicate<String> campusIdValidator) {
        this.campusIdValidator = campusIdValidator != null
                ? campusIdValidator
                : defaultCampusIdValidator();
    }

    private Predicate<String> defaultAlignmentIdValidator() {
        Set<String> ids = yamlSectionKeys("alignments.yml", "alignments");
        return ids.isEmpty() ? id -> true : ids::contains;
    }

    private Predicate<String> defaultCampusIdValidator() {
        Set<String> ids = new java.util.HashSet<>();
        for (Campus campus : Campus.values()) {
            ids.add(campus.getId());
        }
        return ids::contains;
    }

    private Set<String> yamlSectionKeys(String fileName, String sectionName) {
        Set<String> keys = new java.util.HashSet<>();
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            return keys;
        }
        ConfigurationSection section =
                YamlConfiguration.loadConfiguration(file).getConfigurationSection(sectionName);
        if (section == null) {
            return keys;
        }
        for (String key : section.getKeys(false)) {
            keys.add(key.toLowerCase(Locale.ROOT));
        }
        return keys;
    }

    private String lowercase(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

    /** Usable contracts: enabled and passing reference validation. */
    public Map<String, ContractDefinition> getContracts() { return contracts; }
    public ContractDefinition getContract(String id) { return contracts.get(id); }
    public Map<String, ContractDefinition> getDisabledContracts() { return disabledContracts; }
    public Map<String, String> getDisabledReasons() { return disabledReasons; }

    private static final class ContractDisabledException extends RuntimeException {
        private ContractDisabledException(String reason) {
            super(reason);
        }
    }
}
