package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.alignments.config.AlignmentConfigManager;
import com.sunwayMinecraft.alignments.config.AlignmentSettingsConfig;
import com.sunwayMinecraft.alignments.domain.AlignmentDefinition;
import com.sunwayMinecraft.alignments.service.AlignmentChatService;
import com.sunwayMinecraft.alignments.service.AlignmentMembershipCache;
import com.sunwayMinecraft.alignments.service.AlignmentService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Root dispatcher for /align and /ac. Player-facing subcommands live in
 * {@link AlignMemberCommands}, admin subcommands in {@link AlignAdminCommands}.
 */
public class AlignCommands implements CommandExecutor {
  static final String ADMIN_PERMISSION = "sunway.align.admin";
  static final String CHATSPY_PERMISSION = "sunway.align.chatspy";

  private final AlignmentConfigManager configManager;
  private final AlignMemberCommands memberCommands;
  private final AlignChatCommands chatCommands;
  private final AlignAdminCommands adminCommands;

  public AlignCommands(
      AlignmentService service,
      AlignmentConfigManager configManager,
      AlignmentSettingsConfig settings,
      AlignmentChatService chatService,
      AlignmentMembershipCache cache) {
    this.configManager = configManager;
    this.memberCommands =
        new AlignMemberCommands(service, configManager, settings, chatService, cache);
    this.chatCommands = new AlignChatCommands(chatService);
    this.adminCommands = new AlignAdminCommands(service, configManager, settings, cache);
  }

  @Override
  public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    // "/ac <message>" is pure alignment chat
    if (label.equalsIgnoreCase("ac")) {
      chatCommands.handleAcChat(sender, args);
      return true;
    }

    String subCommand = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
    switch (subCommand) {
      case "help" -> sendHelp(sender);
      case "list" -> memberCommands.sendList(sender, args);
      case "join" -> memberCommands.handleJoin(sender, args);
      case "leave" -> memberCommands.handleLeave(sender);
      case "show" -> memberCommands.handleShow(sender, args);
      case "chat" -> chatCommands.handleChat(sender, args);
      case "chatspy" -> chatCommands.handleChatSpy(sender);
      case "set" -> adminCommands.handleSet(sender, args);
      case "clear" -> adminCommands.handleClear(sender, args);
      case "info" -> adminCommands.handleInfo(sender, args);
      case "reload" -> adminCommands.handleReload(sender);
      default -> sender.sendMessage("§cUnknown subcommand. Use /align help");
    }
    return true;
  }

  private void sendHelp(CommandSender sender) {
    boolean admin = sender.hasPermission(ADMIN_PERMISSION);
    sender.sendMessage("§6--- Triple Alliance Alignments ---");
    sender.sendMessage("§e/align list [page]§f - View alignments by grand alliance");
    sender.sendMessage("§e/align join <alignment>§f - Pledge to an alignment");
    sender.sendMessage("§e/align leave§f - Leave your current alignment");
    sender.sendMessage("§e/align show§f - Show your current alignment");
    sender.sendMessage("§e/align chat <message>§f - Chat with your alignment (or /ac <message>)");
    if (sender.hasPermission(CHATSPY_PERMISSION)) {
      sender.sendMessage("§e/align chatspy§f - Toggle alignment chat spy");
    }
    if (admin) {
      sender.sendMessage("§e/align show <player>§f - Show another player's alignment");
      sender.sendMessage("§e/align set <player> <alignment>§f - Force a player's alignment");
      sender.sendMessage("§e/align clear <player>§f - Remove a player's alignment");
      sender.sendMessage("§e/align info <player>§f - Detailed membership info");
      sender.sendMessage("§e/align reload§f - Reload alignment configuration");
    }
  }
}
