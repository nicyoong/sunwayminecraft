package com.sunwayMinecraft.alignments.listener;

import com.sunwayMinecraft.alignments.config.AlignmentConfigManager;
import com.sunwayMinecraft.alignments.config.AlignmentSettingsConfig;
import com.sunwayMinecraft.alignments.domain.AlignmentDefinition;
import com.sunwayMinecraft.alignments.service.AlignmentChatFormatter;
import com.sunwayMinecraft.alignments.service.AlignmentMembershipCache;
import com.sunwayMinecraft.alignments.service.AlignmentMembershipCache.CachedMembership;
import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

/**
 * Prepends the configured alignment prefix to global chat. Integrates with
 * other chat formatting by wrapping the existing renderer instead of
 * replacing it, and leaves chat untouched for players without an alignment.
 */
public class AlignmentChatListener implements Listener {
  private final AlignmentSettingsConfig settings;
  private final AlignmentConfigManager configManager;
  private final AlignmentMembershipCache cache;

  public AlignmentChatListener(
      AlignmentSettingsConfig settings,
      AlignmentConfigManager configManager,
      AlignmentMembershipCache cache) {
    this.settings = settings;
    this.configManager = configManager;
    this.cache = cache;
  }

  public void register(JavaPlugin plugin) {
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onAsyncChat(AsyncChatEvent event) {
    Component prefix = buildPrefix(event.getPlayer());
    if (prefix == null) {
      return;
    }
    ChatRenderer existing = event.renderer();
    event.renderer(
        (source, sourceDisplayName, message, viewer) ->
            prefix.append(existing.render(source, sourceDisplayName, message, viewer)));
  }

  /**
   * Builds the prefix component for a player, or null when no prefix should
   * be applied (no membership, disabled globally, or hidden per config).
   * Public for testing.
   */
  public Component buildPrefix(Player player) {
    if (!settings.isAllowGlobalChatPrefix()) {
      return null;
    }
    Optional<CachedMembership> cached = cache.getOrLoad(player.getUniqueId());
    if (cached.isEmpty() || !"active".equals(cached.get().status())) {
      return null;
    }
    Optional<AlignmentDefinition> definition =
        configManager.getAlignment(cached.get().alignmentId());
    if (definition.isEmpty() || !definition.get().enabled()) {
      if (settings.getDisabledPrefixMode() == AlignmentSettingsConfig.DisabledPrefixMode.NEUTRAL) {
        return AlignmentChatFormatter.legacyToComponent(AlignmentChatFormatter.neutralPrefix());
      }
      return null;
    }
    return AlignmentChatFormatter.renderWithMessage(
        settings.getChatPrefixFormat(),
        AlignmentChatFormatter.placeholders(configManager, settings, definition.get(), player.getName()),
        Component.empty());
  }
}
