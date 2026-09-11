package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.alignments.config.AlignmentConfigManager;
import com.sunwayMinecraft.alignments.config.AlignmentSettingsConfig;
import com.sunwayMinecraft.alignments.domain.AlignmentDefinition;
import com.sunwayMinecraft.alignments.service.AlignmentMembershipCache;
import com.sunwayMinecraft.alignments.service.AlignmentMembershipCache.CachedMembership;
import com.sunwayMinecraft.alignments.service.AlignmentResult;
import com.sunwayMinecraft.alignments.service.AlignmentService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/** Admin /align subcommands: set, clear, info, reload. */
public class AlignAdminCommands {
  private static final String ADMIN_PERMISSION = "sunway.align.admin";
  private static final DateTimeFormatter JOIN_DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

  private final AlignmentService service;
  private final AlignmentConfigManager configManager;
  private final AlignmentSettingsConfig settings;
  private final AlignmentMembershipCache cache;

  AlignAdminCommands(
      AlignmentService service,
      AlignmentConfigManager configManager,
      AlignmentSettingsConfig settings,
      AlignmentMembershipCache cache) {
    this.service = service;
    this.configManager = configManager;
    this.settings = settings;
    this.cache = cache;
  }

  void handleSet(CommandSender sender, String[] args) {
    if (!requireAdmin(sender)) return;
    if (args.length < 3) {
      sender.sendMessage("§cUsage: /align set <player> <alignment>");
      return;
    }
    Player target = requireOnlinePlayer(sender, args[1]);
    if (target == null) return;

    // /align set never applies a cooldown and always overwrites
    AlignmentResult result =
        service.adminSet(target.getUniqueId(), AlignMemberCommands.joinArguments(args, 2));
    switch (result) {
      case JOINED -> {
        String display = configManager
            .getAlignment(AlignMemberCommands.joinArguments(args, 2))
            .map(AlignmentDefinition::displayName)
            .orElse(AlignMemberCommands.joinArguments(args, 2));
        sender.sendMessage("§aSet §f" + target.getName() + "§a to §f" + display + "§a.");
        target.sendMessage("§eYour alignment was set by an admin.");
      }
      case NOT_FOUND -> sender.sendMessage("§cUnknown alignment id. Use /align list.");
      case DISABLED -> sender.sendMessage("§cThat alignment is disabled.");
      case DATABASE_FAILURE ->
          sender.sendMessage("§cCould not update the player's alignment. Check the server log.");
      default -> sender.sendMessage("§cCould not set the alignment.");
    }
  }

  void handleClear(CommandSender sender, String[] args) {
    if (!requireAdmin(sender)) return;
    if (args.length < 2) {
      sender.sendMessage("§cUsage: /align clear <player>");
      return;
    }
    Player target = requireOnlinePlayer(sender, args[1]);
    if (target == null) return;

    AlignmentResult result = service.adminClear(target.getUniqueId());
    switch (result) {
      case LEFT -> {
        sender.sendMessage("§aCleared §f" + target.getName() + "§a's alignment.");
        target.sendMessage("§eYour alignment was removed by an admin.");
      }
      case NOT_ALIGNED ->
          sender.sendMessage("§c" + target.getName() + " is not aligned to any alignment.");
      case DATABASE_FAILURE ->
          sender.sendMessage("§cCould not update the player's alignment. Check the server log.");
      default -> sender.sendMessage("§cCould not clear the alignment.");
    }
  }

  void handleInfo(CommandSender sender, String[] args) {
    if (!requireAdmin(sender)) return;
    if (args.length < 2) {
      sender.sendMessage("§cUsage: /align info <player>");
      return;
    }
    Player target = requireOnlinePlayer(sender, args[1]);
    if (target == null) return;

    sender.sendMessage("§6=== Alignment Info: " + target.getName() + " ===");
    Optional<CachedMembership> cached = cache.getOrLoad(target.getUniqueId());
    if (cached.isEmpty() || !"active".equals(cached.get().status())) {
      sender.sendMessage("§7Unaligned - no active membership.");
      sender.sendMessage("§eCache state: §f" + describeCacheState(target.getUniqueId()));
      return;
    }
    Optional<AlignmentDefinition> definition =
        configManager.getAlignment(cached.get().alignmentId());
    sender.sendMessage(
        "§eAlignment: §f"
            + definition.map(AlignmentDefinition::displayName).orElse(cached.get().alignmentId()));
    sender.sendMessage("§eGrand Alliance: §f" + cached.get().grandAllianceId());
    sender.sendMessage("§eCampus: §f" + cached.get().campusId());
    sender.sendMessage("§eReputation: §f" + cached.get().reputation());
    sender.sendMessage("§eStatus: §f" + cached.get().status());
    sender.sendMessage(
        "§eCache state: §f"
            + describeCacheState(target.getUniqueId())
            + (definition.isPresent() ? "" : " §7(alignment no longer configured)"));
    service
        .getMembership(target.getUniqueId())
        .ifPresent(
            membership ->
                sender.sendMessage(
                    "§eJoined: §f"
                        + JOIN_DATE_FORMAT.format(Instant.ofEpochMilli(membership.joinedAt()))));
  }

  private String describeCacheState(java.util.UUID playerUuid) {
    return cache.get(playerUuid).isPresent() ? "cached" : "database only";
  }

  /** Reloads both alignment config files and revalidates cached memberships. */
  void handleReload(CommandSender sender) {
    if (!requireAdmin(sender)) return;
    configManager.load();
    settings.load();
    cache.revalidate(settings);
    sender.sendMessage(
        "§aAlignment configuration reloaded ("
            + configManager.getAllAlignments().size()
            + " alignments).");
  }

  private boolean requireAdmin(CommandSender sender) {
    if (!sender.hasPermission(ADMIN_PERMISSION)) {
      sender.sendMessage("§cYou do not have permission to use this command.");
      return false;
    }
    return true;
  }

  private Player requireOnlinePlayer(CommandSender sender, String name) {
    Player target = Bukkit.getPlayerExact(name);
    if (target == null) {
      sender.sendMessage("§cPlayer not found: §f" + name);
      return null;
    }
    return target;
  }
}
