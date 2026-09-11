package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.alignments.config.AlignmentPerksConfig;
import com.sunwayMinecraft.alignments.config.AlignmentProgressionConfig;
import com.sunwayMinecraft.alignments.config.AlignmentProgressionConfig.RankDefinition;
import com.sunwayMinecraft.alignments.service.AlignmentPerkService;
import com.sunwayMinecraft.alignments.service.AlignmentRankService;
import com.sunwayMinecraft.alignments.service.AlignmentResult;
import com.sunwayMinecraft.alignments.service.AlignmentScoreService;
import com.sunwayMinecraft.alignments.service.AlignmentSeasonService;
import com.sunwayMinecraft.alignments.service.AlignmentService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Part 3 admin and leaderboard subcommands of /align: leaderboard,
 * season, reputation and perks.
 */
public class AlignProgressionCommands {
  private static final int PAGE_SIZE = 8;
  private static final DateTimeFormatter DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

  private final AlignmentService service;
  private final AlignmentProgressionConfig progressionConfig;
  private final AlignmentPerksConfig perksConfig;
  private final AlignmentRankService rankService;
  private final AlignmentSeasonService seasonService;
  private final AlignmentScoreService scoreService;

  public AlignProgressionCommands(
      AlignmentService service,
      AlignmentProgressionConfig progressionConfig,
      AlignmentPerksConfig perksConfig,
      AlignmentRankService rankService,
      AlignmentSeasonService seasonService,
      AlignmentScoreService scoreService) {
    this.service = service;
    this.progressionConfig = progressionConfig;
    this.perksConfig = perksConfig;
    this.rankService = rankService;
    this.seasonService = seasonService;
    this.scoreService = scoreService;
  }

  // ───────────────────────── leaderboard ─────────────────────────

  void handleLeaderboard(CommandSender sender, String[] args) {
    String view = args.length >= 2 ? args[1].toLowerCase() : "alignments";
    switch (view) {
      case "grand" -> sendGrandLeaderboard(sender, args);
      case "season" -> sendSeasonLeaderboard(sender, args);
      case "player" -> sendPlayerStanding(sender, args);
      case "reload" -> reloadProgression(sender);
      case "alignments", "help" -> sendAlignmentLeaderboard(sender, args);
      default -> {
        sender.sendMessage("§cUnknown leaderboard view: §f" + view);
        sender.sendMessage("§7Views: alignments, grand, season, player, reload");
      }
    }
  }

  private void sendAlignmentLeaderboard(CommandSender sender, String[] args) {
    List<Map.Entry<String, long[]>> entries = new ArrayList<>(
        service.getAlignmentTotals().entrySet());
    if (entries.isEmpty()) {
      sender.sendMessage("§7No alignment reputation recorded yet.");
      return;
    }
    entries.sort(Comparator.comparingLong((Map.Entry<String, long[]> e) -> e.getValue()[0]).reversed());
    sendPage(sender, "Alignment Reputation", entries.size(), args, index -> {
      Map.Entry<String, long[]> entry = entries.get(index);
      return "§e#" + (index + 1) + " §f" + entry.getKey() + " §7- §e"
          + entry.getValue()[0] + " reputation §7(" + entry.getValue()[1] + " members)";
    });
  }

  private void sendGrandLeaderboard(CommandSender sender, String[] args) {
    List<Map.Entry<String, Long>> entries = new ArrayList<>(
        scoreService.getGrandAllianceScores().entrySet());
    entries.sort(Comparator.comparingLong(Map.Entry<String, Long>::getValue).reversed());
    sendPage(sender, "Grand Alliance Scores", entries.size(), args, index -> {
      Map.Entry<String, Long> entry = entries.get(index);
      return "§e#" + (index + 1) + " §f" + entry.getKey() + " §7- §6score " + entry.getValue();
    });
  }

  private void sendSeasonLeaderboard(CommandSender sender, String[] args) {
    String seasonId = seasonService.getCurrentSeason()
        .map(s -> s.seasonId()).orElse(null);
    if (seasonId == null) {
      sender.sendMessage("§cSeasons are disabled or storage is unavailable.");
      return;
    }
    List<Map.Entry<String, long[]>> entries = new ArrayList<>(
        seasonService.getSeasonAlignmentTotals().entrySet());
    if (entries.isEmpty()) {
      sender.sendMessage("§7No reputation recorded in season " + seasonId + " yet.");
      return;
    }
    entries.sort(Comparator.comparingLong((Map.Entry<String, long[]> e) -> e.getValue()[0]).reversed());
    sendPage(sender, "Season " + seasonId + " Leaders", entries.size(), args, index -> {
      Map.Entry<String, long[]> entry = entries.get(index);
      return "§e#" + (index + 1) + " §f" + entry.getKey() + " §7- §e"
          + entry.getValue()[0] + " reputation §7(" + entry.getValue()[1] + " members)";
    });
  }

  private void sendPlayerStanding(CommandSender sender, String[] args) {
    Player target;
    if (args.length >= 3) {
      if (!sender.hasPermission(AlignCommands.ADMIN_PERMISSION)) {
        sender.sendMessage("§cYou do not have permission to view other players' standing.");
        return;
      }
      target = Bukkit.getPlayerExact(args[2]);
      if (target == null) {
        sender.sendMessage("§cPlayer not found: §f" + args[2]);
        return;
      }
    } else if (sender instanceof Player) {
      target = (Player) sender;
    } else {
      sender.sendMessage("§cConsole must specify a player: /align leaderboard player <player>");
      return;
    }

    Optional<String> alignmentId = service.getAlignmentId(target.getUniqueId());
    if (alignmentId.isEmpty()) {
      sender.sendMessage("§7" + target.getName() + " is unaligned - no standing.");
      return;
    }
    var cached = service.getCache().getOrLoad(target.getUniqueId());
    String grandAllianceId = cached.map(c -> c.grandAllianceId()).orElse(null);
    int reputation = cached.map(c -> c.reputation()).orElse(0);
    int position = service.getReputationPosition(target.getUniqueId());
    Optional<RankDefinition> rank = rankService.resolveRank(reputation, grandAllianceId);
    sender.sendMessage("§6=== " + target.getName() + " ===");
    sender.sendMessage("§eAlignment: §f" + alignmentId.get());
    sender.sendMessage("§eReputation: §f" + reputation);
    sender.sendMessage("§eStanding: §f#" + position + " §7in " + alignmentId.get());
    sender.sendMessage(
        "§eRank: §f" + rank.map(RankDefinition::displayName).orElse("none"));
  }

  private void reloadProgression(CommandSender sender) {
    if (!requireAdmin(sender)) return;
    progressionConfig.load();
    perksConfig.load();
    sender.sendMessage("§aAlignment progression and perks configuration reloaded.");
  }

  // ───────────────────────── season ─────────────────────────

  void handleSeason(CommandSender sender, String[] args) {
    String sub = args.length >= 2 ? args[1].toLowerCase() : "info";
    switch (sub) {
      case "end" -> {
        if (!requireAdmin(sender)) return;
        sender.sendMessage(seasonService.endSeason("admin command")
            ? "§aSeason ended and the next season started."
            : "§cCould not end the season. Check the server log.");
      }
      case "reset" -> {
        if (!requireAdmin(sender)) return;
        sender.sendMessage(seasonService.resetSeason()
            ? "§aSeason timer reset; a new season started."
            : "§cCould not reset the season. Check the server log.");
      }
      case "snapshot" -> {
        if (!requireAdmin(sender)) return;
        sender.sendMessage(seasonService.forceSnapshot()
            ? "§aSeason snapshot recorded."
            : "§cCould not snapshot the season. Check the server log.");
      }
      case "info" -> sendSeasonInfo(sender);
      default -> sender.sendMessage("§cUsage: /align season <info|end|reset|snapshot>");
    }
  }

  private void sendSeasonInfo(CommandSender sender) {
    var season = seasonService.getCurrentSeason();
    if (season.isEmpty()) {
      sender.sendMessage("§cSeasons are disabled or storage is unavailable.");
      return;
    }
    long length = progressionConfig.getSeasonLengthMillis();
    long endsAt = season.get().startedAt() + length;
    sender.sendMessage("§6=== Season " + season.get().seasonId() + " ===");
    sender.sendMessage("§eStarted: §f" + DATE_FORMAT.format(Instant.ofEpochMilli(season.get().startedAt())));
    sender.sendMessage(length > 0
        ? "§eEnds: §f" + DATE_FORMAT.format(Instant.ofEpochMilli(endsAt))
        : "§eLength: §fendless (seasons disabled)");
  }

  // ───────────────────────── reputation ─────────────────────────

  void handleReputation(CommandSender sender, String[] args) {
    if (!requireAdmin(sender)) return;
    if (args.length < 4) {
      sender.sendMessage("§cUsage: /align reputation <add|remove|set> <player> <amount>");
      return;
    }
    String action = args[1].toLowerCase();
    Player target = Bukkit.getPlayerExact(args[2]);
    if (target == null) {
      sender.sendMessage("§cPlayer not found: §f" + args[2]);
      return;
    }
    int amount;
    try {
      amount = Integer.parseInt(args[3]);
    } catch (NumberFormatException e) {
      sender.sendMessage("§cInvalid amount: §f" + args[3]);
      return;
    }
    UUID targetUuid = target.getUniqueId();
    AlignmentResult result = switch (action) {
      case "add" -> service.adjustReputation(targetUuid, amount);
      case "remove" -> service.adjustReputation(targetUuid, -Math.abs(amount));
      case "set" -> service.setReputation(targetUuid, amount);
      default -> null;
    };
    if (result == null) {
      sender.sendMessage("§cUsage: /align reputation <add|remove|set> <player> <amount>");
      return;
    }
    switch (result) {
      case JOINED -> sender.sendMessage("§aReputation of §f" + target.getName()
          + "§a is now §e" + service.getMembership(targetUuid).map(m -> m.reputation()).orElse(0) + "§a.");
      case NOT_ALIGNED -> sender.sendMessage("§c" + target.getName() + " is not aligned.");
      case DATABASE_FAILURE -> sender.sendMessage("§cCould not update reputation. Check the server log.");
      default -> sender.sendMessage("§cCould not update reputation.");
    }
  }

  // ───────────────────────── perks ─────────────────────────

  void handlePerks(CommandSender sender, String[] args) {
    if (!requireAdmin(sender)) return;
    String sub = args.length >= 2 ? args[1].toLowerCase() : "help";
    if ("reload".equals(sub)) {
      perksConfig.load();
      sender.sendMessage("§aAlignment perks configuration reloaded ("
          + perksConfig.getPerks().size() + " perks).");
    } else {
      sender.sendMessage("§cUsage: /align perks reload");
    }
  }

  // ───────────────────────── helpers ─────────────────────────

  private boolean requireAdmin(CommandSender sender) {
    if (!sender.hasPermission(AlignCommands.ADMIN_PERMISSION)) {
      sender.sendMessage("§cYou do not have permission to use this command.");
      return false;
    }
    return true;
  }

  private void sendPage(CommandSender sender, String title, int total, String[] args,
      java.util.function.IntFunction<String> lineFor) {
    int pageCount = Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
    int page = parsePage(args.length >= 3 ? args[2] : "1", pageCount);
    sender.sendMessage("§6=== " + title + " (page " + page + "/" + pageCount + ") ===");
    int start = (page - 1) * PAGE_SIZE;
    for (int i = start; i < Math.min(start + PAGE_SIZE, total); i++) {
      sender.sendMessage(lineFor.apply(i));
    }
  }

  private int parsePage(String raw, int max) {
    try {
      return Math.min(Math.max(1, Integer.parseInt(raw)), max);
    } catch (NumberFormatException e) {
      return 1;
    }
  }
}
