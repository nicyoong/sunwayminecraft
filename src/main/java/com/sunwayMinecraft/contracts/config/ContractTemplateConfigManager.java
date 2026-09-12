package com.sunwayMinecraft.contracts.config;

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
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Loads contract-templates.yml: blueprints for dynamically generated
 * emergency contracts. Templates share the contracts.yml field vocabulary;
 * reference validation (endpoints, alignments) happens at generation time so
 * a template can still list an endpoint the server has not loaded yet.
 */
public class ContractTemplateConfigManager {
    private final JavaPlugin plugin;
    private final File configFile;
    private final Map<String, ContractDefinition> templates = new HashMap<>();

    public ContractTemplateConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "contract-templates.yml");
        if (!configFile.exists()) {
            plugin.saveResource("contract-templates.yml", false);
        }
    }

    public void load() {
        templates.clear();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        ConfigurationSection section = config.getConfigurationSection("templates");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            try {
                templates.put(key.toLowerCase(Locale.ROOT), parse(key, section));
            } catch (RuntimeException e) {
                plugin.getLogger().warning("Skipping malformed contract template '" + key + "': "
                        + e.getMessage());
            }
        }
    }

    private ContractDefinition parse(String key, ConfigurationSection section) {
        ContractCategory category = ContractCategory.valueOf(section
                .getString(key + ".contract_type",
                        section.getString(key + ".category", "EMERGENCY")).toUpperCase(Locale.ROOT));
        ContractObjectiveType objectiveType;
        String objectiveTypeValue = section.getString(key + ".objective_type");
        if (objectiveTypeValue == null) {
            objectiveType = ContractDefinition.defaultObjectiveType(category);
        } else {
            objectiveType = ContractObjectiveType.valueOf(objectiveTypeValue.toUpperCase(Locale.ROOT));
        }
        Map<Material, Integer> materials = new HashMap<>();
        ConfigurationSection matSection = section.getConfigurationSection(key + ".required_materials");
        if (matSection != null) {
            for (String matKey : matSection.getKeys(false)) {
                Material material = Material.matchMaterial(matKey);
                if (material != null) materials.put(material, matSection.getInt(matKey));
            }
        }
        List<String> forbidden = new ArrayList<>();
        if (section.isList(key + ".forbidden_alignment")) {
            for (String v : section.getStringList(key + ".forbidden_alignment")) {
                forbidden.add(v.toLowerCase(Locale.ROOT));
            }
        }
        ContractAlignmentRule rule = new ContractAlignmentRule(
                lowercase(section.getString(key + ".required_alignment")),
                lowercase(section.getString(key + ".recommended_alignment")), forbidden);
        ContractCampusRoute route = new ContractCampusRoute(
                lowercase(section.getString(key + ".origin_campus")),
                lowercase(section.getString(key + ".destination_campus")),
                lowercase(section.getString(key + ".origin_alignment")),
                lowercase(section.getString(key + ".destination_alignment")));
        return new ContractDefinition(key.toLowerCase(Locale.ROOT), category,
                section.getString(key + ".display_name", key),
                section.getString(key + ".description", ""),
                section.getDouble(key + ".reward_money", 0.0),
                section.getLong(key + ".duration_minutes", 20),
                section.getLong(key + ".cooldown_minutes", 10),
                section.getString(key + ".start_endpoint"),
                section.getString(key + ".end_endpoint"),
                materials,
                section.getString(key + ".objective_description", ""),
                objectiveType, rule, route,
                section.getLong(key + ".reward_reputation", 0), true);
    }

    private String lowercase(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

    public Map<String, ContractDefinition> getTemplates() { return templates; }
    public ContractDefinition getTemplate(String id) {
        return id == null ? null : templates.get(id.toLowerCase(Locale.ROOT));
    }
}
