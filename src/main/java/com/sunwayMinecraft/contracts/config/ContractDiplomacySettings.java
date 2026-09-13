package com.sunwayMinecraft.contracts.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/** Loads contract-diplomacy.yml with safe defaults; sabotage is off unless enabled. */
public class ContractDiplomacySettings {
    private final JavaPlugin plugin;
    private final File configFile;

    private boolean enabled = true;
    private int influencePerCompletion = 5;
    private int crossCampusBonusInfluence = 0;
    private double diplomaticContractMultiplier = 1.0;
    private int emergencyContractInfluenceBonus = 0;
    private boolean sabotageEnabled = false;
    private long sabotageCooldownSeconds = 600;
    private double sabotageSuccessChance = 0.5;
    private int sabotageFailurePenalty = 0;
    private double sabotageItemCost = 0.0;
    private int maxActiveDynamicContracts = 3;
    private long emergencyIntervalMinutes = 0;
    private String emergencyTemplate = "";

    public ContractDiplomacySettings(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "contract-diplomacy.yml");
        if (!configFile.exists()) {
            plugin.saveResource("contract-diplomacy.yml", false);
        }
    }

    public void load() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        enabled = config.getBoolean("enabled", true);
        influencePerCompletion = config.getInt("influence_per_completion", 5);
        crossCampusBonusInfluence = config.getInt("cross_campus_bonus_influence", 0);
        diplomaticContractMultiplier = config.getDouble("diplomatic_contract_multiplier", 1.0);
        emergencyContractInfluenceBonus = config.getInt("emergency_contract_influence_bonus", 0);
        sabotageEnabled = config.getBoolean("sabotage_enabled", false);
        sabotageCooldownSeconds = config.getLong("sabotage_cooldown_seconds", 600);
        sabotageSuccessChance = clamp01(config.getDouble("sabotage_success_chance", 0.5));
        sabotageFailurePenalty = config.getInt("sabotage_failure_penalty", 0);
        sabotageItemCost = config.getDouble("sabotage_item_cost", 0.0);
        maxActiveDynamicContracts = config.getInt("max_active_dynamic_contracts", 3);
        emergencyIntervalMinutes = config.getLong("emergency_generation.interval_minutes", 0);
        emergencyTemplate = config.getString("emergency_generation.template", "");
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    public boolean isEnabled() { return enabled; }
    public int getInfluencePerCompletion() { return influencePerCompletion; }
    public int getCrossCampusBonusInfluence() { return crossCampusBonusInfluence; }
    public double getDiplomaticContractMultiplier() { return diplomaticContractMultiplier; }
    public int getEmergencyContractInfluenceBonus() { return emergencyContractInfluenceBonus; }
    public boolean isSabotageEnabled() { return sabotageEnabled; }
    public long getSabotageCooldownSeconds() { return sabotageCooldownSeconds; }
    public double getSabotageSuccessChance() { return sabotageSuccessChance; }
    public int getSabotageFailurePenalty() { return sabotageFailurePenalty; }
    public double getSabotageItemCost() { return sabotageItemCost; }
    public int getMaxActiveDynamicContracts() { return maxActiveDynamicContracts; }
    public long getEmergencyIntervalMinutes() { return emergencyIntervalMinutes; }
    public String getEmergencyTemplate() { return emergencyTemplate; }
}
