package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.alignments.config.AlignmentConfigManager;
import com.sunwayMinecraft.alignments.config.AlignmentSettingsConfig;
import com.sunwayMinecraft.alignments.domain.AlignmentDefinition;
import com.sunwayMinecraft.alignments.domain.GrandAlliance;
import com.sunwayMinecraft.alignments.service.AlignmentChatFormatter;
import com.sunwayMinecraft.alignments.service.AlignmentChatService;
import com.sunwayMinecraft.alignments.service.AlignmentMembershipCache;
import com.sunwayMinecraft.alignments.service.AlignmentResult;
import com.sunwayMinecraft.alignments.service.AlignmentService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Player-facing /align subcommands: list, join, leave, show, chat, chatspy. */
public class AlignMemberCommands {
  private static final String ADMIN_PERMISSION = "sunway.align.admin";
  static final int LIST_PAGE_SIZE = 8;
  private static final DateTimeFormatter JOIN_DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

  private final AlignmentService service;
  private final AlignmentConfigManager configManager;
  private final AlignmentSettingsConfig settings;
  private final AlignmentChatService chatService;
  private final AlignmentMembershipCache cache;

  AlignMemberCommands(
      AlignmentService service,
      AlignmentConfigManager configManager,
      AlignmentSettingsConfig settings,
      AlignmentChatService chatService,
      AlignmentMembershipCache cache) {
    this.service = service;
    this.configManager = configManager;
    this.settings = settings;
    this.chatService = chatService;
    this.cache = cache;
  }

  void sendList(CommandSender sender, String[] args) {
    boolean admin = sender.hasPermission(ADMIN_PERMISSION);
    List<AlignmentDefinition> visible = new ArrayList<>();
    for (GrandAlliance alliance : GrandAlliance.values()) {
      for (AlignmentDefinition definition : configManager.getAlignmentsByGrandAlliance(alliance)) {
        if (definition.enabled() || admin) {
          visible.add(definition);
        }
      }
    }
    if (visible.isEmpty()) {
      sender.sendMessage("§cNo alignments are configured.");
      return;
    }

    int page = parsePage(args.length >= 2 ? args[1] : "1");
    int totalPages = (visible.size() + LIST_PAGE_SIZE - 1) / LIST_PAGE_SIZE;
    page = Math.min(Math.max(1, page), totalPages);
    sender.sendMessage(
        "§6=== Triple Alliance Alignments (page " + page + "/" + totalPages + ") ===");

    Map<String, Integer> counts = service.getAlignmentMemberCounts();
    GrandAlliance lastAlliance = null;
    int start = (page - 1) * LIST_PAGE_SIZE;
    for (int i = start; i < Math.min(start + LIST_PAGE_SIZE, visible.size()); i++) {
      AlignmentDefinition definition = visible.get(i);
      if (definition.grandAlliance() != lastAlliance) {
        lastAlliance = definition.grandAlliance();
        sender.sendMessage(
            AlignmentChatFormatter.colorize(AlignmentChatFormatter.allianceHeader(configManager, lastAlliance)));
      }
      StringBuilder line =
          new StringBuilder(
              "§7 - §f" + definition.displayName() + " §7(" + definition.homeCampus().getId() + ")");
      Integer count = counts.get(definition.id());
      if (count != null) {
        line.append(" §8[").append(count).append(count == 1 ? " member" : " members").append("]");
      }
      if (!definition.enabled()) {
        line.append(" §8[DISABLED]");
      }
      sender.sendMessage(line.toString());
    }
  }

  void handleJoin(CommandSender sender, String[] args) {
    if (!(sender instanceof Player)) {
      sender.sendMessage("§cOnly players can join an alignment!");
      return;
    }
    if (args.length < 2) {
      sender.sendMessage("§cUsage: /align join <alignment>");
      return;
    }
    if (!service.isAvailable()) {
      sender.sendMessage("§cThe alignment system is currently unavailable. Try again later.");
      return;
    }

    // Allow multi-word ids such as "/align join azure hearth"
    String alignmentInput = joinArguments(args, 1);
    Player player = (Player) sender;
    boolean bypass =
        player.hasPermission("sunway.align.bypass.cooldown")
            || (settings.isAllowAdminBypass() && player.hasPermission(ADMIN_PERMISSION));
    AlignmentResult result = service.join(player.getUniqueId(), alignmentInput, bypass);
    switch (result) {
      case JOINED -> announceJoin(player, alignmentInput);
      case ALREADY_ALIGNED -> player.sendMessage("§cYou are already aligned to that alignment.");
      case NOT_FOUND -> {
        player.sendMessage("§cUnknown alignment: §f" + alignmentInput);
        player.sendMessage("§7Use /align list to see available alignments.");
      }
      case DISABLED -> player.sendMessage("§cThat alignment is not accepting members.");
      case COOLDOWN_ACTIVE ->
          player.sendMessage(
              "§cYou must wait §f"
                  + formatDuration(service.getRemainingCooldownSeconds(player.getUniqueId()))
                  + "§c before joining another alignment.");
      case DATABASE_FAILURE ->
          player.sendMessage("§cCould not save your alignment. Try again later.");
      default -> player.sendMessage("§cCould not join the alignment.");
    }
  }

  private void announceJoin(Player player, String alignmentInput) {
    Optional<AlignmentDefinition> definition = configManager.getAlignment(alignmentInput);
    if (definition.isEmpty()) {
      player.sendMessage("§aYou have pledged to §f" + alignmentInput + "§a!");
      return;
    }
    AlignmentDefinition def = definition.get();
    player.sendMessage("§aYou have pledged to §f" + def.displayName() + "§a!");
    player.sendMessage(
        AlignmentChatFormatter.colorize(
            "§7Grand Alliance: " + AlignmentChatFormatter.allianceName(configManager, def.grandAlliance())));
    player.sendMessage("§7Home Campus: §f" + def.homeCampus().getId());
    player.sendMessage("§7" + def.description());

    if (!settings.isShowJoinMessage()) {
      return;
    }
    for (Player online : Bukkit.getOnlinePlayers()) {
      if (!online.getUniqueId().equals(player.getUniqueId())) {
        online.sendMessage(
            "§b" + player.getName() + " has pledged to §f" + def.displayName() + "§b!");
      }
    }
  }

  void handleLeave(CommandSender sender) {
    if (!(sender instanceof Player)) {
      sender.sendMessage("§cOnly players can leave an alignment!");
      return;
    }
    if (!service.isAvailable()) {
      sender.sendMessage("§cThe alignment system is currently unavailable. Try again later.");
      return;
    }

    Player player = (Player) sender;
    AlignmentResult result = service.leave(player.getUniqueId());
    switch (result) {
      case LEFT -> {
        player.sendMessage("§aYou have left your alignment. You are now unaligned.");
        if (settings.isShowLeaveMessage()) {
          for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.getUniqueId().equals(player.getUniqueId())) {
              online.sendMessage("§b" + player.getName() + " is now unaligned.");
            }
          }
        }
      }
      case NOT_ALIGNED -> player.sendMessage("§cYou are not aligned to any alignment.");
      case DATABASE_FAILURE ->
          player.sendMessage("§cCould not update your alignment. Try again later.");
      default -> player.sendMessage("§cCould not leave the alignment.");
    }
  }

  void handleShow(CommandSender sender, String[] args) {
    java.util.UUID targetUuid;
    String targetName;
    if (args.length >= 2) {
      if (!sender.hasPermission(ADMIN_PERMISSION)) {
        sender.sendMessage("§cYou do not have permission to view other players' alignments.");
        return;
      }
      Player target = Bukkit.getPlayerExact(args[1]);
      if (target == null) {
        sender.sendMessage("§cPlayer not found: §f" + args[1]);
        return;
      }
      targetUuid = target.getUniqueId();
      targetName = target.getName();
    } else {
      if (!(sender instanceof Player)) {
        sender.sendMessage("§cConsole must specify a player: /align show <player>");
        return;
      }
      Player player = (Player) sender;
      targetUuid = player.getUniqueId();
      targetName = player.getName();
    }

    sender.sendMessage("§6=== Alignment: " + targetName + " ===");
    Optional<AlignmentMembershipCache.CachedMembership> cached = cache.getOrLoad(targetUuid);
    if (cached.isEmpty() || !"active".equals(cached.get().status())) {
      sender.sendMessage("§7Unaligned - has not pledged to any alignment yet.");
      return;
    }
    Optional<AlignmentDefinition> definition =
        configManager.getAlignment(cached.get().alignmentId());
    if (definition.isEmpty()) {
      sender.sendMessage(
          "§eAlignment: §f" + cached.get().alignmentId() + " §7(no longer configured)");
      return;
    }
    AlignmentDefinition def = definition.get();
    sender.sendMessage("§eAlignment: §f" + def.displayName());
    sender.sendMessage(
        AlignmentChatFormatter.colorize(
            "§eGrand Alliance: " + AlignmentChatFormatter.allianceName(configManager, def.grandAlliance())));
    sender.sendMessage("§eHome Campus: §f" + def.homeCampus().getId());
    service
        .getMembership(targetUuid)
        .ifPresent(
            membership ->
                sender.sendMessage(
                    "§eJoined: §f"
                        + JOIN_DATE_FORMAT.format(Instant.ofEpochMilli(membership.joinedAt()))));
  }

  static String joinArguments(String[] args, int from) {
    return String.join("_", java.util.Arrays.copyOfRange(args, from, args.length))
        .toLowerCase(Locale.ROOT);
  }

  private static int parsePage(String raw) {
    try {
      return Integer.parseInt(raw);
    } catch (NumberFormatException e) {
      return 1;
    }
  }

  static String formatDuration(long seconds) {
    if (seconds <= 0) {
      return "0s";
    }
    long hours = seconds / 3600;
    long minutes = (seconds % 3600) / 60;
    long secs = seconds % 60;
    StringBuilder builder = new StringBuilder();
    if (hours > 0) builder.append(hours).append("h ");
    if (minutes > 0) builder.append(minutes).append("m ");
    if (secs > 0 || builder.isEmpty()) builder.append(secs).append("s");
    return builder.toString().trim();
  }
}
