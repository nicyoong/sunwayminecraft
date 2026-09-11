package com.sunwayMinecraft.alignments.service;

import com.sunwayMinecraft.alignments.config.AlignmentConfigManager;
import com.sunwayMinecraft.alignments.config.AlignmentSettingsConfig;
import com.sunwayMinecraft.alignments.domain.AlignmentDefinition;
import com.sunwayMinecraft.alignments.domain.GrandAlliance;
import com.sunwayMinecraft.alignments.domain.GrandAllianceDefinition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Shared legacy-string formatting helpers for alignment chat features. */
public final class AlignmentChatFormatter {
  private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

  private AlignmentChatFormatter() {}

  /** Converts legacy ampersand codes to section codes. */
  public static String colorize(String text) {
    return text.replace('&', '§');
  }

  /** Replaces {placeholder} tokens with the supplied values. */
  public static String replacePlaceholders(String format, Map<String, String> values) {
    String result = format;
    for (Map.Entry<String, String> entry : values.entrySet()) {
      result = result.replace("{" + entry.getKey() + "}", entry.getValue());
    }
    return result;
  }

  public static Component legacyToComponent(String legacy) {
    return LEGACY.deserialize(colorize(legacy));
  }

  /**
   * Renders a format string that may contain a {message} placeholder: text
   * before and after the placeholder is deserialized as legacy text and the
   * message component is inserted in place. Without a placeholder the
   * message is appended after a space.
   */
  public static Component renderWithMessage(
      String format, Map<String, String> placeholderValues, Component message) {
    String rendered = colorize(replacePlaceholders(format, placeholderValues));
    int messageIndex = rendered.indexOf("{message}");
    if (messageIndex < 0) {
      return LEGACY.deserialize(rendered).append(Component.space()).append(message);
    }
    String before = rendered.substring(0, messageIndex);
    String after = rendered.substring(messageIndex + "{message}".length());
    return LEGACY.deserialize(before).append(message).append(LEGACY.deserialize(after));
  }

  /**
   * Builds the placeholder values shared by the global chat prefix and
   * alignment chat: player, alignment, grand alliance (full and short) and
   * campus. Alliance/alignment names come in their configured chat colours.
   */
  public static Map<String, String> placeholders(
      AlignmentConfigManager configManager,
      AlignmentSettingsConfig settings,
      AlignmentDefinition definition,
      String playerName) {
    Map<String, String> values = new HashMap<>();
    values.put("player", playerName);
    values.put("alignment", coloredAlignmentName(settings, definition));
    values.put("campus", definition.homeCampus().getId());
    values.put("alignment_plain", definition.displayName());

    String allianceDisplay = definition.grandAlliance().getId();
    String allianceColor = "&f";
    String allianceShort = allianceDisplay;
    String[] words = allianceDisplay.split("_");
    if (words.length > 0) {
      allianceShort = capitalize(words[0]);
    }
    var allianceDefinition = configManager.getAllianceDefinition(definition.grandAlliance());
    if (allianceDefinition.isPresent()) {
      GrandAllianceDefinition def = allianceDefinition.get();
      allianceDisplay = def.displayName();
      allianceColor = def.chatColor();
      String[] displayWords = allianceDisplay.split(" ");
      if (displayWords.length > 0) {
        allianceShort = displayWords[0];
      }
    }
    values.put("grand_alliance", AlignmentChatFormatter.colorize(allianceColor) + allianceDisplay + "§r");
    values.put("grand_alliance_short", AlignmentChatFormatter.colorize(allianceColor) + allianceShort + "§r");
    values.put("grand_alliance_plain", allianceDisplay);
    return values;
  }

  static String coloredAlignmentName(AlignmentSettingsConfig settings, AlignmentDefinition definition) {
    String color =
        definition.chatColor() != null && !definition.chatColor().isBlank()
            ? definition.chatColor()
            : settings.getAlignmentChatColor();
    return colorize(color) + definition.displayName() + "§r";
  }

  public static String neutralPrefix() {
    return "&7[Unaligned] &r";
  }

  /** Coloured alliance display name with its chat colour applied. */
  public static String allianceName(AlignmentConfigManager configManager, GrandAlliance alliance) {
    Optional<GrandAllianceDefinition> definition = configManager.getAllianceDefinition(alliance);
    String color = definition.map(GrandAllianceDefinition::chatColor).orElse("&f");
    String name = definition.map(GrandAllianceDefinition::displayName).orElse(alliance.getId());
    return "&e" + colorize(color) + name;
  }

  /** Coloured alliance header line such as "Concordat of the Dawn:". */
  public static String allianceHeader(AlignmentConfigManager configManager, GrandAlliance alliance) {
    return allianceName(configManager, alliance) + "&e:";
  }

  private static String capitalize(String word) {
    return word.isEmpty() ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1);
  }

  static String humanizeEnum(Enum<?> value) {
    String[] words = value.name().toLowerCase(Locale.ROOT).split("_");
    StringBuilder builder = new StringBuilder();
    for (String word : words) {
      if (!builder.isEmpty()) builder.append(' ');
      builder.append(capitalize(word));
    }
    return builder.toString();
  }
}
