package com.sunwayMinecraft.alignments.service;

import com.sunwayMinecraft.alignments.config.AlignmentConfigManager;
import com.sunwayMinecraft.alignments.config.AlignmentSettingsConfig;
import com.sunwayMinecraft.alignments.domain.AlignmentDefinition;
import com.sunwayMinecraft.alignments.service.AlignmentMembershipCache.CachedMembership;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Alignment-only chat: delivers a message to every online member of the
 * sender's alignment. Admins with the chatspy permission can toggle
 * themselves as silent observers.
 */
public class AlignmentChatService {
  public enum ChatResult { SENT, NO_ALIGNMENT, DISABLED, EMPTY_MESSAGE }

  private final AlignmentSettingsConfig settings;
  private final AlignmentConfigManager configManager;
  private final AlignmentMembershipCache cache;
  private final Set<UUID> spies = new HashSet<>();

  public AlignmentChatService(
      AlignmentSettingsConfig settings,
      AlignmentConfigManager configManager,
      AlignmentMembershipCache cache) {
    this.settings = settings;
    this.configManager = configManager;
    this.cache = cache;
  }

  public ChatResult sendAlignmentChat(Player sender, String message) {
    if (!settings.isAllowAlignmentChat()) {
      return ChatResult.DISABLED;
    }
    if (message == null || message.isBlank()) {
      return ChatResult.EMPTY_MESSAGE;
    }

    Optional<CachedMembership> cached = cache.getOrLoad(sender.getUniqueId());
    if (cached.isEmpty() || !"active".equals(cached.get().status())) {
      return ChatResult.NO_ALIGNMENT;
    }
    Optional<AlignmentDefinition> definition =
        configManager.getAlignment(cached.get().alignmentId());
    if (definition.isEmpty() || !definition.get().enabled()) {
      return ChatResult.NO_ALIGNMENT;
    }

    Component rendered =
        renderAlignmentMessage(sender.getName(), definition.get(), message);
    Set<UUID> delivered = new HashSet<>();
    for (Player recipient : Bukkit.getOnlinePlayers()) {
      if (!isSameActiveAlignment(recipient.getUniqueId(), cached.get().alignmentId())) {
        continue;
      }
      recipient.sendMessage(rendered);
      delivered.add(recipient.getUniqueId());
    }

    Component spyCopy =
        Component.text("[SPY] ", net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY)
            .append(rendered);
    for (Player spy : Bukkit.getOnlinePlayers()) {
      if (delivered.contains(spy.getUniqueId())) {
        continue;
      }
      if (isSpy(spy.getUniqueId()) && spy.hasPermission("sunway.align.chatspy")) {
        spy.sendMessage(spyCopy);
      }
    }
    return ChatResult.SENT;
  }

  /** Toggles the chatspy flag for a player; returns the new state. */
  public boolean toggleSpy(UUID playerUuid) {
    if (spies.contains(playerUuid)) {
      spies.remove(playerUuid);
      return false;
    }
    spies.add(playerUuid);
    return true;
  }

  public boolean isSpy(UUID playerUuid) {
    return spies.contains(playerUuid);
  }

  public int getSpyCount() {
    return spies.size();
  }

  /** Renders an alignment chat message; public for testing. */
  public Component renderAlignmentMessage(
      String playerName, AlignmentDefinition definition, String message) {
    return AlignmentChatFormatter.renderWithMessage(
        settings.getAlignmentChatFormat(),
        AlignmentChatFormatter.placeholders(
            configManager, settings, definition, playerName, null, null),
        Component.text(message));
  }

  private boolean isSameActiveAlignment(UUID playerUuid, String alignmentId) {
    return cache
        .getOrLoad(playerUuid)
        .filter(cached -> "active".equals(cached.status()))
        .filter(cached -> cached.alignmentId().equalsIgnoreCase(alignmentId))
        .isPresent();
  }
}
