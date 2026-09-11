package com.sunwayMinecraft.alignments.listener;

import com.sunwayMinecraft.alignments.config.AlignmentConfigManager;
import com.sunwayMinecraft.alignments.config.AlignmentProgressionConfig;
import com.sunwayMinecraft.alignments.config.AlignmentSettingsConfig;
import com.sunwayMinecraft.alignments.domain.AlignmentDefinition;
import com.sunwayMinecraft.alignments.service.AlignmentChatFormatter;
import com.sunwayMinecraft.alignments.service.AlignmentMembershipCache;
import com.sunwayMinecraft.alignments.service.AlignmentMembershipCache.CachedMembership;
import com.sunwayMinecraft.alignments.service.AlignmentRankService;
import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Optional;

/**
 * Prepends the configured alignment prefix and reputation rank suffix to
 * global chat. Integrates with other chat formatting by wrapping the
 * existing renderer instead of replacing it, and leaves chat untouched for
 * players without an alignment or when both toggles are off.
 */
public class AlignmentChatListener implements Listener {
  private final AlignmentSettingsConfig settings;
  private final AlignmentConfigManager configManager;
  private final AlignmentMembershipCache cache;
  private final AlignmentRankService rankService;
  private final AlignmentProgressionConfig progressionConfig;

  public AlignmentChatListener(
      AlignmentSettingsConfig settings,
      AlignmentConfigManager configManager,
      AlignmentMembershipCache cache,
      AlignmentRankService rankService,
      AlignmentProgressionConfig progressionConfig) {
    this.settings = settings;
    this.configManager = configManager;
    this.cache = cache;
    this.rankService = rankService;
    this.progressionConfig = progressionConfig;
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
   * Builds the prefix/suffix component for a player, or null when nothing
   * should be applied. Public for testing.
   */
  public Component buildPrefix(Player player) {
    boolean prefixEnabled = settings.isAllowGlobalChatPrefix();
    boolean suffixEnabled = settings.isAllowChatRankSuffix();
    if (!prefixEnabled && !suffixEnabled) {
      return null;
    }
    Optional<CachedMembership> cached = cache.getOrLoad(player.getUniqueId());
    if (cached.isEmpty() || !"active".equals(cached.get().status())) {
      return null;
    }
    Optional<AlignmentDefinition> definition =
        configManager.getAlignment(cached.get().alignmentId());
    if (definition.isEmpty() || !definition.get().enabled()) {
      if (prefixEnabled
          && settings.getDisabledPrefixMode() == AlignmentSettingsConfig.DisabledPrefixMode.NEUTRAL) {
        return AlignmentChatFormatter.legacyToComponent(AlignmentChatFormatter.neutralPrefix());
      }
      return null;
    }
    Optional<AlignmentProgressionConfig.RankDefinition> rank =
        resolveRank(cached.get(), definition.get());
    String rankDisplay =
        rank.map(AlignmentProgressionConfig.RankDefinition::displayName).orElse(null);
    String rankSuffix = rank.map(AlignmentProgressionConfig.RankDefinition::chatSuffix).orElse("");

    Map<String, String> placeholders = AlignmentChatFormatter.placeholders(
        configManager, settings, definition.get(), player.getName(), rankDisplay, rankSuffix);

    Component result = Component.empty();
    if (prefixEnabled) {
      result = result.append(AlignmentChatFormatter.renderWithMessage(
          settings.getChatPrefixFormat(), placeholders, Component.empty()));
    }
    if (suffixEnabled && !rankSuffix.isBlank()) {
      result = result.append(AlignmentChatFormatter.renderWithMessage(
          settings.getChatRankSuffixFormat(), placeholders, Component.empty()));
    }
    // nothing rendered (prefix off and the rank has no suffix) means no change
    return result == Component.empty() ? null : result;
  }

  private Optional<AlignmentProgressionConfig.RankDefinition> resolveRank(
      CachedMembership cached, AlignmentDefinition definition) {
    if (cached.rankId() == null) {
      return Optional.empty();
    }
    return progressionConfig.getRank(definition.grandAlliance().getId(), cached.rankId());
  }
}
