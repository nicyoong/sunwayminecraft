package com.sunwayMinecraft.districts.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * Settings for district control contests, loaded from
 * district-control-settings.yml. Missing or invalid values fall back to
 * safe defaults.
 */
public class DistrictControlSettingsConfig {
    private static final String FILE_NAME = "district-control-settings.yml";

    private final JavaPlugin plugin;
    private final File configFile;

    private boolean enabled = true;
    private int contestTickIntervalSeconds = 5;
    private double controlPointRadius = 0;
    private double pointsPerPlayerPerTick = 2.0;
    private int minimumPlayersToContest = 1;
    private int maximumControlPoints = 500;
    private int pointsRequiredToCapture = 100;
    private boolean decayWhenUncontested = true;
    private boolean allowNeutralReset = true;
    private boolean broadcastControlChanges = true;
    private boolean broadcastContestStart = true;
    private boolean broadcastContestEnd = true;
    private boolean broadcastCapture = true;
    private boolean contestLogEnabled = true;
    private int captureCooldownSeconds = 300;
    private boolean rewardControllersOnCapture = false;
    private double rewardMoney = 100.0;
    private int rewardReputation = 0;
    private boolean rewardPlayersPresent = true;
    private int minimumContributionPoints = 10;
    private int rewardCooldownSeconds = 3600;
    private String captureMessage = "§6{district} §7has been captured by §f{alignment}§7!";
    private String contestStartMessage = "§e{district} §7is being contested by §f{alignment}§7!";
    private String contestEndMessage = "§7The contest for §e{district} §7has ended.";
    private String contestActionBar = "District contest: {alignment} {percent}%";

    public DistrictControlSettingsConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), FILE_NAME);
        if (!configFile.exists()) {
            plugin.saveResource(FILE_NAME, false);
        }
    }

    public void load() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        enabled = config.getBoolean("enabled", true);
        contestTickIntervalSeconds = Math.max(1, config.getInt("contest_tick_interval_seconds", 5));
        controlPointRadius = Math.max(0, config.getDouble("control_point_radius", 0));
        pointsPerPlayerPerTick = Math.max(0, config.getDouble("points_per_player_per_tick", 2.0));
        minimumPlayersToContest = Math.max(1, config.getInt("minimum_players_to_contest", 1));
        maximumControlPoints = Math.max(1, config.getInt("maximum_control_points", 500));
        pointsRequiredToCapture = Math.max(1, config.getInt("points_required_to_capture", 100));
        decayWhenUncontested = config.getBoolean("decay_when_uncontested", true);
        allowNeutralReset = config.getBoolean("allow_neutral_reset", true);
        broadcastControlChanges = config.getBoolean("broadcast_control_changes", true);
        broadcastContestStart = config.getBoolean("broadcast_contest_start", true);
        broadcastContestEnd = config.getBoolean("broadcast_contest_end", true);
        broadcastCapture = config.getBoolean("broadcast_capture", true);
        contestLogEnabled = config.getBoolean("contest_log_enabled", true);
        captureCooldownSeconds = Math.max(0, config.getInt("capture_cooldown_seconds", 300));
        rewardControllersOnCapture = config.getBoolean("reward_controllers_on_capture", false);
        rewardMoney = Math.max(0, config.getDouble("reward_money", 100.0));
        rewardReputation = Math.max(0, config.getInt("reward_reputation", 0));
        rewardPlayersPresent = config.getBoolean("reward_players_present", true);
        minimumContributionPoints = Math.max(0, config.getInt("minimum_contribution_points", 10));
        rewardCooldownSeconds = Math.max(0, config.getInt("reward_cooldown_seconds", 3600));
        captureMessage = config.getString("capture_message", captureMessage);
        contestStartMessage = config.getString("contest_start_message", contestStartMessage);
        contestEndMessage = config.getString("contest_end_message", contestEndMessage);
        contestActionBar = config.getString("contest_action_bar", contestActionBar);
    }

    /** Reloads the settings file (admin action). */
    public void reload() {
        load();
    }

    public boolean isEnabled() { return enabled; }
    public int getContestTickIntervalSeconds() { return contestTickIntervalSeconds; }
    public double getControlPointRadius() { return controlPointRadius; }
    public double getPointsPerPlayerPerTick() { return pointsPerPlayerPerTick; }
    public int getMinimumPlayersToContest() { return minimumPlayersToContest; }
    public int getMaximumControlPoints() { return maximumControlPoints; }
    public int getPointsRequiredToCapture() { return pointsRequiredToCapture; }
    public boolean isDecayWhenUncontested() { return decayWhenUncontested; }
    public boolean isAllowNeutralReset() { return allowNeutralReset; }
    public boolean isBroadcastControlChanges() { return broadcastControlChanges; }
    public boolean isBroadcastContestStart() { return broadcastContestStart; }
    public boolean isBroadcastContestEnd() { return broadcastContestEnd; }
    public boolean isBroadcastCapture() { return broadcastCapture; }
    public boolean isContestLogEnabled() { return contestLogEnabled; }
    public int getCaptureCooldownSeconds() { return captureCooldownSeconds; }
    public boolean isRewardControllersOnCapture() { return rewardControllersOnCapture; }
    public double getRewardMoney() { return rewardMoney; }
    public int getRewardReputation() { return rewardReputation; }
    public boolean isRewardPlayersPresent() { return rewardPlayersPresent; }
    public int getMinimumContributionPoints() { return minimumContributionPoints; }
    public int getRewardCooldownSeconds() { return rewardCooldownSeconds; }
    public String getCaptureMessage() { return captureMessage; }
    public String getContestStartMessage() { return contestStartMessage; }
    public String getContestEndMessage() { return contestEndMessage; }
    public String getContestActionBar() { return contestActionBar; }
}
