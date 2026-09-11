package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.alignments.config.AlignmentConfigManager;
import com.sunwayMinecraft.alignments.domain.AlignmentDefinition;
import com.sunwayMinecraft.alignments.domain.AlignmentMembership;
import com.sunwayMinecraft.alignments.domain.GrandAlliance;
import com.sunwayMinecraft.alignments.domain.GrandAllianceDefinition;
import com.sunwayMinecraft.alignments.service.AlignmentResult;
import com.sunwayMinecraft.alignments.service.AlignmentService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AlignCommands implements CommandExecutor, TabCompleter {
  private static final String ADMIN_PERMISSION = "sunway.align.admin";
  private static final DateTimeFormatter JOIN_DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

  private final AlignmentService service;
  private final AlignmentConfigManager configManager;

  public AlignCommands(AlignmentService service, AlignmentConfigManager configManager) {
    this.service = service;
    this.configManager = configManager;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    String subCommand = args.length == 0 ? "help" : args[0].toLowerCase();
    switch (subCommand) {
      case "help":
        sendHelp(sender);
        return true;
      case "list":
        sendList(sender);
        return true;
      case "join":
        handleJoin(sender, args);
        return true;
      case "leave":
        handleLeave(sender);
        return true;
      case "show":
        handleShow(sender, args);
        return true;
      default:
        sender.sendMessage("§cUnknown subcommand. Use /align help");
        return true;
    }
  }

  private void sendHelp(CommandSender sender) {
    sender.sendMessage("§6--- Triple Alliance Alignments ---");
    sender.sendMessage("§e/align list§f - View all alignments by grand alliance");
    sender.sendMessage("§e/align join <alignment>§f - Pledge to an alignment");
    sender.sendMessage("§e/align leave§f - Leave your current alignment");
    sender.sendMessage("§e/align show§f - Show your current alignment");
    if (sender.hasPermission(ADMIN_PERMISSION)) {
      sender.sendMessage("§e/align show <player>§f - Show another player's alignment");
    }
    sender.sendMessage("§e/align help§f - Show this help");
  }

  private void sendList(CommandSender sender) {
    sender.sendMessage("§6=== Triple Alliance Alignments ===");
    boolean admin = sender.hasPermission(ADMIN_PERMISSION);
    for (GrandAlliance alliance : GrandAlliance.values()) {
      List<AlignmentDefinition> definitions = configManager.getAlignmentsByGrandAlliance(alliance);
      if (definitions.isEmpty()) {
        continue;
      }
      Optional<GrandAllianceDefinition> allianceDefinition =
          configManager.getAllianceDefinition(alliance);
      String allianceColor =
          allianceDefinition.map(GrandAllianceDefinition::chatColor).orElse("&f");
      String allianceName =
          allianceDefinition.map(GrandAllianceDefinition::displayName).orElse(alliance.getId());

      sender.sendMessage(colorize(allianceColor + allianceName + ":"));
      for (AlignmentDefinition definition : definitions) {
        if (!definition.enabled() && !admin) {
          continue;
        }
        String line =
            "§7 - §f"
                + definition.displayName()
                + " §7(" + definition.homeCampus().getId() + ")";
        if (!definition.enabled()) {
          line += " §8[DISABLED]";
        }
        sender.sendMessage(colorize(line));
      }
    }
  }

  private void handleJoin(CommandSender sender, String[] args) {
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
    String alignmentInput =
        String.join("_", Arrays.copyOfRange(args, 1, args.length)).toLowerCase();

    Player player = (Player) sender;
    AlignmentResult result = service.join(player.getUniqueId(), alignmentInput);
    switch (result) {
      case JOINED -> {
        Optional<AlignmentDefinition> definition = configManager.getAlignment(alignmentInput);
        String display =
            definition.map(AlignmentDefinition::displayName).orElse(alignmentInput);
        player.sendMessage("§aYou have pledged to §f" + display + "§a!");
        definition.ifPresent(
            def -> player.sendMessage("§7" + def.description()));
      }
      case ALREADY_ALIGNED ->
          player.sendMessage("§cYou are already aligned to that alignment.");
      case NOT_FOUND -> {
        player.sendMessage("§cUnknown alignment: §f" + alignmentInput);
        player.sendMessage("§7Use /align list to see available alignments.");
      }
      case DISABLED -> player.sendMessage("§cThat alignment is not accepting members.");
      case DATABASE_FAILURE ->
          player.sendMessage("§cCould not save your alignment. Try again later.");
      default -> player.sendMessage("§cCould not join the alignment.");
    }
  }

  private void handleLeave(CommandSender sender) {
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
      case LEFT -> player.sendMessage("§aYou have left your alignment. You are now unaligned.");
      case NOT_ALIGNED -> player.sendMessage("§cYou are not aligned to any alignment.");
      case DATABASE_FAILURE ->
          player.sendMessage("§cCould not update your alignment. Try again later.");
      default -> player.sendMessage("§cCould not leave the alignment.");
    }
  }

  private void handleShow(CommandSender sender, String[] args) {
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
      sendAlignmentInfo(sender, target.getName(), target.getUniqueId());
      return;
    }

    if (!(sender instanceof Player)) {
      sender.sendMessage("§cConsole must specify a player: /align show <player>");
      return;
    }
    Player player = (Player) sender;
    sendAlignmentInfo(player, player.getName(), player.getUniqueId());
  }

  private void sendAlignmentInfo(CommandSender sender, String targetName, UUID targetUuid) {
    sender.sendMessage("§6=== Alignment: " + targetName + " ===");
    if (!service.isAvailable()) {
      sender.sendMessage("§cThe alignment system is currently unavailable. Try again later.");
      return;
    }

    Optional<AlignmentMembership> membership = service.getMembership(targetUuid);
    if (membership.isEmpty()) {
      sender.sendMessage("§7Unaligned - has not pledged to any alignment yet.");
      return;
    }

    Optional<AlignmentDefinition> definition =
        configManager.getAlignment(membership.get().alignmentId());
    if (definition.isEmpty()) {
      sender.sendMessage("§eAlignment: §f" + membership.get().alignmentId() + " §7(no longer configured)");
      return;
    }

    AlignmentDefinition def = definition.get();
    String allianceColor = "&f";
    String allianceName = def.grandAlliance().getId();
    Optional<GrandAllianceDefinition> allianceDefinition =
        configManager.getAllianceDefinition(def.grandAlliance());
    if (allianceDefinition.isPresent()) {
      allianceColor = allianceDefinition.get().chatColor();
      allianceName = allianceDefinition.get().displayName();
    }

    sender.sendMessage(colorize("&eAlignment: &f" + def.displayName()));
    sender.sendMessage(colorize("&eGrand Alliance: " + allianceColor + allianceName));
    sender.sendMessage("&eHome Campus: §f" + def.homeCampus().getId());
    sender.sendMessage("§eJoined: §f" + JOIN_DATE_FORMAT.format(Instant.ofEpochMilli(membership.get().joinedAt())));
  }

  @Override
  public List<String> onTabComplete(
      CommandSender sender, Command cmd, String alias, String[] args) {
    List<String> completions = new ArrayList<>();
    if (args.length == 1) {
      completions.addAll(List.of("help", "list", "join", "leave", "show"));
    } else if (args.length == 2) {
      if ("join".equalsIgnoreCase(args[0])) {
        configManager.getEnabledAlignments().forEach(def -> completions.add(def.id()));
      } else if ("show".equalsIgnoreCase(args[0]) && sender.hasPermission(ADMIN_PERMISSION)) {
        Bukkit.getOnlinePlayers().forEach(player -> completions.add(player.getName()));
      }
    }
    completions.removeIf(entry -> !entry.toLowerCase().startsWith(args[args.length - 1].toLowerCase()));
    return completions;
  }

  private static String colorize(String text) {
    return text.replace('&', '§');
  }
}
