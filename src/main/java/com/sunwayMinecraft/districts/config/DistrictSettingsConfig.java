package com.sunwayMinecraft.districts.config;

import com.sunwayMinecraft.districts.domain.DistrictType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Enforcement and feedback settings for districts, loaded from
 * district-settings.yml. Missing or invalid values fall back to safe
 * defaults.
 */
public class DistrictSettingsConfig {
    /** Which channel receives district enter feedback. */
    public enum EnterMessageMode { CHAT, ACTION_BAR, NONE }

    /** Interaction permission flags for one district type. */
    public record InteractionFlags(
            boolean allowInteract, boolean allowContainers, boolean allowRedstone,
            boolean allowDoors, boolean allowCrafting) {
        public static final InteractionFlags ALL =
                new InteractionFlags(true, true, true, true, true);
        public static final InteractionFlags NONE =
                new InteractionFlags(false, false, false, false, false);
    }

    private static final String FILE_NAME = "district-settings.yml";
    private static final InteractionFlags DEFAULT_FLAGS =
            new InteractionFlags(true, true, true, true, true);

    private final JavaPlugin plugin;
    private final File configFile;

    private boolean enforceBuildRules = true;
    private boolean enforceInteractionRules = true;
    private boolean enforceResidencyRules = true;
    private boolean archivedReadOnly = true;
    private boolean sanctuaryNoPvp = true;
    private boolean sanctuaryNoBuild = true;
    private boolean sanctuaryNoMobDamage = true;
    private boolean contestedAllowAccess = false;
    private EnterMessageMode enterMessageMode = EnterMessageMode.ACTION_BAR;
    private int enterMessageCooldownSeconds = 0;
    private boolean showOwner = true;
    private boolean showContestedStatus = true;
    private boolean showDistrictType = true;
    private boolean verboseDenialLogging = false;
    private final Map<DistrictType, InteractionFlags> interactionFlags =
            new EnumMap<>(DistrictType.class);
    private InteractionFlags defaultFlags = DEFAULT_FLAGS;

    public DistrictSettingsConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), FILE_NAME);
        if (!configFile.exists()) {
            plugin.saveResource(FILE_NAME, false);
        }
    }

    public void load() {
        interactionFlags.clear();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        enforceBuildRules = config.getBoolean("enforce_build_rules", true);
        enforceInteractionRules = config.getBoolean("enforce_interaction_rules", true);
        enforceResidencyRules = config.getBoolean("enforce_residency_rules", true);
        archivedReadOnly = config.getBoolean("archived_read_only", true);
        sanctuaryNoPvp = config.getBoolean("sanctuary_no_pvp", true);
        sanctuaryNoBuild = config.getBoolean("sanctuary_no_build", true);
        sanctuaryNoMobDamage = config.getBoolean("sanctuary_no_mob_damage", true);
        contestedAllowAccess = config.getBoolean("contested_allow_access", false);
        enterMessageMode = parseMode(config.getString("enter_message_mode", "action_bar"));
        enterMessageCooldownSeconds =
                Math.max(0, config.getInt("enter_message_cooldown_seconds", 0));
        showOwner = config.getBoolean("show_owner", true);
        showContestedStatus = config.getBoolean("show_contested_status", true);
        showDistrictType = config.getBoolean("show_district_type", true);
        verboseDenialLogging = config.getBoolean("verbose_denial_logging", false);

        ConfigurationSection defaults = config.getConfigurationSection("interaction-defaults");
        if (defaults != null) {
            for (String key : defaults.getKeys(false)) {
                if ("default".equalsIgnoreCase(key)) {
                    defaultFlags = readFlags(defaults, key);
                    continue;
                }
                try {
                    DistrictType type = DistrictType.valueOf(key.toUpperCase(Locale.ROOT));
                    interactionFlags.put(type, readFlags(defaults, key));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("[Districts] Unknown district type in "
                            + "interaction-defaults: " + key);
                }
            }
        }
    }

    private EnterMessageMode parseMode(String raw) {
        try {
            return EnterMessageMode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("[Districts] Invalid enter_message_mode '" + raw
                    + "'; using action_bar.");
            return EnterMessageMode.ACTION_BAR;
        }
    }

    private InteractionFlags readFlags(ConfigurationSection section, String key) {
        ConfigurationSection flags = section.getConfigurationSection(key);
        if (flags == null) {
            return DEFAULT_FLAGS;
        }
        return new InteractionFlags(
                flags.getBoolean("allow_interact", true),
                flags.getBoolean("allow_containers", true),
                flags.getBoolean("allow_redstone", true),
                flags.getBoolean("allow_doors", true),
                flags.getBoolean("allow_crafting", true));
    }

    /** Flags for a district type, falling back to the default set. */
    public InteractionFlags interactionFlagsFor(DistrictType type) {
        if (type != null && interactionFlags.containsKey(type)) {
            return interactionFlags.get(type);
        }
        return defaultFlags;
    }

    public boolean isEnforceBuildRules() { return enforceBuildRules; }
    public boolean isEnforceInteractionRules() { return enforceInteractionRules; }
    public boolean isEnforceResidencyRules() { return enforceResidencyRules; }
    public boolean isArchivedReadOnly() { return archivedReadOnly; }
    public boolean isSanctuaryNoPvp() { return sanctuaryNoPvp; }
    public boolean isSanctuaryNoBuild() { return sanctuaryNoBuild; }
    public boolean isSanctuaryNoMobDamage() { return sanctuaryNoMobDamage; }
    public boolean isContestedAllowAccess() { return contestedAllowAccess; }
    public EnterMessageMode getEnterMessageMode() { return enterMessageMode; }
    public int getEnterMessageCooldownSeconds() { return enterMessageCooldownSeconds; }
    public boolean isShowOwner() { return showOwner; }
    public boolean isShowContestedStatus() { return showContestedStatus; }
    public boolean isShowDistrictType() { return showDistrictType; }
    public boolean isVerboseDenialLogging() { return verboseDenialLogging; }
}
