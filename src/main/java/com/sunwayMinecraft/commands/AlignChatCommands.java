package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.alignments.service.AlignmentChatService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Alignment-chat subcommands of /align: chat, chatspy (and /ac). */
public class AlignChatCommands {
  private static final String CHATSPY_PERMISSION = "sunway.align.chatspy";

  private final AlignmentChatService chatService;

  AlignChatCommands(AlignmentChatService chatService) {
    this.chatService = chatService;
  }

  void handleChat(CommandSender sender, String[] args) {
    if (!(sender instanceof Player)) {
      sender.sendMessage("§cOnly players can use alignment chat!");
      return;
    }
    if (args.length < 2) {
      sender.sendMessage("§cUsage: /align chat <message>");
      return;
    }
    deliverAlignmentChat(
        (Player) sender,
        String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)));
  }

  /** Handles "/ac <message>" where every argument is message content. */
  void handleAcChat(CommandSender sender, String[] args) {
    if (!(sender instanceof Player)) {
      sender.sendMessage("§cOnly players can use alignment chat!");
      return;
    }
    if (args.length == 0) {
      sender.sendMessage("§cUsage: /ac <message>");
      return;
    }
    deliverAlignmentChat((Player) sender, String.join(" ", args));
  }

  void handleChatSpy(CommandSender sender) {
    if (!(sender instanceof Player)) {
      sender.sendMessage("§cOnly players can toggle alignment chat spy!");
      return;
    }
    if (!sender.hasPermission(CHATSPY_PERMISSION)) {
      sender.sendMessage("§cYou do not have permission to spy on alignment chat.");
      return;
    }
    boolean enabled = chatService.toggleSpy(((Player) sender).getUniqueId());
    sender.sendMessage(enabled ? "§aAlignment chat spy enabled." : "§eAlignment chat spy disabled.");
  }

  private void deliverAlignmentChat(Player player, String message) {
    switch (chatService.sendAlignmentChat(player, message)) {
      case SENT -> {}
      case NO_ALIGNMENT ->
          player.sendMessage("§cYou must join an alignment before using alignment chat.");
      case DISABLED -> player.sendMessage("§cAlignment chat is currently disabled.");
      case EMPTY_MESSAGE -> player.sendMessage("§cUsage: /align chat <message>");
    }
  }
}
