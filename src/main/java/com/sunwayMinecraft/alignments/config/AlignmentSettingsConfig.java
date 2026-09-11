package com.sunwayMinecraft.alignments.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Locale;

/**
 * Runtime settings for the alignment system, loaded from
 * alignments-settings.yml. Any missing or invalid value falls back to a
 * safe default so a broken settings file can never break chat.
 */
public class AlignmentSettingsConfig {
  /** Behaviour when a cached alignment disappears or is disabled on reload. */
  public enum StaleMembershipAction { KEEP, UNALIGN }

  /** Global chat prefix behaviour for disabled or vanished alignments. */
  public enum DisabledPrefixMode { HIDE, NEUTRAL }

  private final JavaPlugin plugin;
  private final File configFile;

  private boolean allowAlignmentChat = true;
  private boolean allowGlobalChatPrefix = true;
  private String chatPrefixFormat = "&7[{grand_alliance_short} | {alignment}&7] &r";
  private String alignmentChatFormat = "&7[{alignment}&7] &f{player}&7: &f{message}";
  private String alignmentChatColor = "&b";
  private long switchCooldownSeconds = 0;
  private boolean cooldownAppliesToLeave = false;
  private boolean allowAdminBypass = true;
  private boolean showJoinMessage = true;
  private boolean showLeaveMessage = true;
  private DisabledPrefixMode disabledPrefixMode = DisabledPrefixMode.HIDE;
  private StaleMembershipAction staleMembershipAction = StaleMembershipAction.KEEP;

  public AlignmentSettingsConfig(JavaPlugin plugin) {
    this.plugin = plugin;
    this.configFile = new File(plugin.getDataFolder(), "alignments-settings.yml");
    if (!configFile.exists()) {
      plugin.saveResource("alignments-settings.yml", false);
    }
  }

  public void load() {
    YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

    allowAlignmentChat = config.getBoolean("allow_alignment_chat", true);
    allowGlobalChatPrefix = config.getBoolean("allow_global_chat_prefix", true);
    chatPrefixFormat = config.getString("chat_prefix_format", chatPrefixFormat);
    alignmentChatFormat = config.getString("alignment_chat_format", alignmentChatFormat);
    alignmentChatColor = config.getString("alignment_chat_color", alignmentChatColor);
    switchCooldownSeconds = Math.max(0, config.getLong("switch_cooldown_seconds", 0));
    cooldownAppliesToLeave = config.getBoolean("cooldown_applies_to_leave", false);
    allowAdminBypass = config.getBoolean("allow_admin_bypass", true);
    showJoinMessage = config.getBoolean("show_join_message", true);
    showLeaveMessage = config.getBoolean("show_leave_message", true);
    disabledPrefixMode =
        parseEnum(config.getString("disabled_alignment_prefix_mode"), DisabledPrefixMode.HIDE,
            "disabled_alignment_prefix_mode");
    staleMembershipAction =
        parseEnum(config.getString("stale_membership_action"), StaleMembershipAction.KEEP,
            "stale_membership_action");
  }

  private <T extends Enum<T>> T parseEnum(String raw, T fallback, String setting) {
    if (raw == null || raw.isBlank()) {
      return fallback;
    }
    try {
      return Enum.valueOf(fallback.getDeclaringClass(), raw.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      plugin.getLogger()
          .warning("Invalid value '" + raw + "' for " + setting + " in alignments-settings.yml; "
              + "using default " + fallback.name().toLowerCase(Locale.ROOT));
      return fallback;
    }
  }

  public boolean isAllowAlignmentChat() {
    return allowAlignmentChat;
  }

  public boolean isAllowGlobalChatPrefix() {
    return allowGlobalChatPrefix;
  }

  public String getChatPrefixFormat() {
    return chatPrefixFormat;
  }

  public String getAlignmentChatFormat() {
    return alignmentChatFormat;
  }

  public String getAlignmentChatColor() {
    return alignmentChatColor;
  }

  public long getSwitchCooldownSeconds() {
    return switchCooldownSeconds;
  }

  public boolean isCooldownAppliesToLeave() {
    return cooldownAppliesToLeave;
  }

  public boolean isAllowAdminBypass() {
    return allowAdminBypass;
  }

  public boolean isShowJoinMessage() {
    return showJoinMessage;
  }

  public boolean isShowLeaveMessage() {
    return showLeaveMessage;
  }

  public DisabledPrefixMode getDisabledPrefixMode() {
    return disabledPrefixMode;
  }

  public StaleMembershipAction getStaleMembershipAction() {
    return staleMembershipAction;
  }
}
