package com.sunwayMinecraft.contracts.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class SettingsConfigManager {
    private final JavaPlugin plugin;
    private final File configFile;

    private int maxActiveContracts = 3;
    private boolean remoteAcceptEnabled = false;
    private boolean allowMultipleSameContract = false;
    private long abandonmentCooldownSeconds = 0;
    private double recommendedBonusMoney = 0.0;
    private long recommendedBonusReputation = 0;
    private double crossCampusBonusMoney = 0.0;
    private long crossCampusBonusReputation = 0;
    private int abandonmentReputationPenalty = 0;
    private int diplomaticEmergencyReputationPenalty = 0;

    public SettingsConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "contract-settings.yml");
        if (!configFile.exists()) {
            plugin.saveResource("contract-settings.yml", false);
        }
    }

    public void load() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        maxActiveContracts = config.getInt("max_active_contracts", 3);
        remoteAcceptEnabled = config.getBoolean("remote_accept_enabled", false);
        allowMultipleSameContract = config.getBoolean("allow_multiple_same_contract", false);
        abandonmentCooldownSeconds = config.getLong("abandonment_cooldown_seconds", 0);
        recommendedBonusMoney = config.getDouble("reward.recommended_alignment_bonus_money", 0.0);
        recommendedBonusReputation = config.getLong("reward.recommended_alignment_bonus_reputation", 0);
        crossCampusBonusMoney = config.getDouble("reward.cross_campus_bonus_money", 0.0);
        crossCampusBonusReputation = config.getLong("reward.cross_campus_bonus_reputation", 0);
        abandonmentReputationPenalty = config.getInt("abandonment.reputation_penalty", 0);
        diplomaticEmergencyReputationPenalty =
                config.getInt("abandonment.diplomatic_emergency_reputation_penalty", 0);
    }

    public int getMaxActiveContracts() { return maxActiveContracts; }
    public boolean isRemoteAcceptEnabled() { return remoteAcceptEnabled; }
    public boolean isAllowMultipleSameContract() { return allowMultipleSameContract; }
    public long getAbandonmentCooldownSeconds() { return abandonmentCooldownSeconds; }
    public double getRecommendedBonusMoney() { return recommendedBonusMoney; }
    public long getRecommendedBonusReputation() { return recommendedBonusReputation; }
    public double getCrossCampusBonusMoney() { return crossCampusBonusMoney; }
    public long getCrossCampusBonusReputation() { return crossCampusBonusReputation; }
    public int getAbandonmentReputationPenalty() { return abandonmentReputationPenalty; }
    public int getDiplomaticEmergencyReputationPenalty() { return diplomaticEmergencyReputationPenalty; }
}
