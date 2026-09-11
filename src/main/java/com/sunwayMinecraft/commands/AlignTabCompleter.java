package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.alignments.config.AlignmentConfigManager;
import com.sunwayMinecraft.alignments.domain.AlignmentDefinition;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Tab completion for /align: subcommands filtered by permission, alignment
 * ids for join/set, online player names for admin lookups. Alignment chat
 * never completes message content.
 */
public class AlignTabCompleter implements TabCompleter {
  private static final String ADMIN_PERMISSION = "sunway.align.admin";
  private static final String CHATSPY_PERMISSION = "sunway.align.chatspy";

  private final AlignmentConfigManager configManager;

  public AlignTabCompleter(AlignmentConfigManager configManager) {
    this.configManager = configManager;
  }

  @Override
  public List<String> onTabComplete(
      CommandSender sender, Command cmd, String alias, String[] args) {
    List<String> completions = new ArrayList<>();
    boolean admin = sender.hasPermission(ADMIN_PERMISSION);
    if (args.length == 1) {
      completions.addAll(List.of("help", "list", "join", "leave", "show", "chat"));
      if (sender.hasPermission(CHATSPY_PERMISSION)) {
        completions.add("chatspy");
      }
      if (admin) {
        completions.addAll(List.of("set", "clear", "info", "reload"));
      }
    } else if (args.length == 2) {
      switch (args[0].toLowerCase(Locale.ROOT)) {
        case "join" -> completions.addAll(alignmentIds());
        case "show", "info", "clear" -> {
          if (admin || "show".equalsIgnoreCase(args[0])) {
            completions.addAll(onlinePlayerNames());
          }
        }
        case "set" -> {
          if (admin) {
            completions.addAll(onlinePlayerNames());
          }
        }
        default -> {}
      }
    } else if (args.length == 3 && "set".equalsIgnoreCase(args[0]) && admin) {
      completions.addAll(alignmentIds());
    }
    completions.removeIf(
        entry ->
            !entry.toLowerCase(Locale.ROOT)
                .startsWith(args[args.length - 1].toLowerCase(Locale.ROOT)));
    return completions;
  }

  private List<String> alignmentIds() {
    List<String> ids = new ArrayList<>();
    for (AlignmentDefinition definition : configManager.getEnabledAlignments()) {
      ids.add(definition.id());
    }
    return ids;
  }

  private List<String> onlinePlayerNames() {
    List<String> names = new ArrayList<>();
    for (Player player : Bukkit.getOnlinePlayers()) {
      names.add(player.getName());
    }
    return names;
  }
}
