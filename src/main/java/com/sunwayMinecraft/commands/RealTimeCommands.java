package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.realtime.RealTimeManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.time.ZoneId;

/**
 * The RealTimeCommands class handles commands that display the server's current time and date in
 * different time zones.
 *
 * <p>This class implements the {@link CommandExecutor} interface, allowing it to process the
 * following commands:
 *
 * <ul>
 *   <li><code>/servertime</code> – shows current time and date in local zone (Singapore Time,
 *       UTC+8).
 *   <li><code>/servertimeutc</code> – shows current time and date in UTC.
 * </ul>
 *
 * <p>Key functionality includes:
 *
 * <ul>
 *   <li>Retrieving and formatting time and date information for specified time zones.
 *   <li>Sending formatted time and date messages to command senders.
 * </ul>
 *
 * <p>The main methods provided by this class are:
 *
 * <ul>
 *   <li><code>onCommand</code> – routes incoming commands to the appropriate handler.
 *   <li><code>sendTimeMessage</code> – formats and sends the time/date message to the sender.
 * </ul>
 */
public class RealTimeCommands implements CommandExecutor {
  private final RealTimeManager realTimeManager;

  /**
   * Creates a new RealTimeCommands handler.
   *
   * @param realTimeManager the manager responsible for retrieving and formatting time and date
   *     information
   */
  public RealTimeCommands(RealTimeManager realTimeManager) {
    this.realTimeManager = realTimeManager;
  }

  /**
   * Processes incoming commands related to server time display.
   *
   * <p>The method performs the following steps:
   *
   * <ol>
   *   <li>Checks the name of the executed command (case-insensitive).
   *   <li>If the command is <code>servertime</code>, it sends the time and date in local zone
   *       (UTC+8).
   *   <li>If the command is <code>servertimeutc</code>, it sends the time and date in UTC.
   *   <li>If the command name does not match, returns false to indicate it was not handled.
   * </ol>
   *
   * <p>This command executor centralizes time-related commands for players and admins.
   *
   * @param sender the source of the command (player, console, etc.)
   * @param cmd the command being executed
   * @param label the alias of the command used
   * @param args any provided command arguments (currently ignored)
   * @return {@code true} if the command was handled; {@code false} otherwise
   */
  @Override
  public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    switch (cmd.getName().toLowerCase()) {
      case "servertime":
        sendTimeMessage(sender, realTimeManager.getLocalZone(), "Singapore Time (UTC+8)");
        return true;

      case "servertimeutc":
        sendTimeMessage(sender, realTimeManager.getUTCZone(), "UTC Time");
        return true;
    }
    return false;
  }

  /**
   * Sends the formatted current time and date to the specified command sender.
   *
   * <p>This method performs the following steps:
   *
   * <ol>
   *   <li>Retrieves the formatted time string for the given time zone.
   *   <li>Retrieves the formatted date string for the given time zone.
   *   <li>Constructs and sends two messages to the sender:
   *       <ul>
   *         <li>One for the current time.
   *         <li>One for the current date.
   *       </ul>
   * </ol>
   *
   * @param sender the recipient of the time message
   * @param zone the time zone to use when formatting time and date
   * @param title a descriptive title to prefix the message (e.g., "UTC Time")
   */
  private void sendTimeMessage(CommandSender sender, ZoneId zone, String title) {
    String time = realTimeManager.getFormattedTime(zone);
    String date = realTimeManager.getFormattedDate(zone);

    sender.sendMessage("§6§l" + title + " §r§7» §aCurrent time: §e" + time);
    sender.sendMessage("§6§l" + title + " §r§7» §aCurrent date: §e" + date);
  }
}
