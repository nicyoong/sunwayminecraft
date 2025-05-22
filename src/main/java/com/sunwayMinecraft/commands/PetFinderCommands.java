package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.petfinder.PetFinderManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.util.Arrays;
import java.util.UUID;

/**
 * The {@code PetFinderCommands} class handles commands related to finding pets in the Minecraft plugin.
 * It processes two main commands:
 * <ul>
 *   <li><b>/findpets</b> - Searches for pets near a specified or invoking player.</li>
 *   <li><b>/findpetsinarea</b> - Searches for pets within a user-defined bounding box (admin only),
 *       optionally targeting a specific player.</li>
 * </ul>
 *
 * <p>Key functionality includes:
 * <ul>
 *   <li>Permission checks to ensure the sender has the required {@code petfinder.admin} permission for
 *       targeting other players or performing area searches.</li>
 *   <li>Parsing of player targets, either from command arguments or defaulting to the invoking player.</li>
 *   <li>Parsing of a 3D bounding box from six coordinate arguments, normalizing minima and maxima.</li>
 *   <li>Delegating search logic to the {@link PetFinderManager}.</li>
 * </ul>
 *
 * <p>Commands are routed based on the command name, with each handler method providing clear
 * usage messages, permission checks, and error handling.
 */
public class PetFinderCommands implements CommandExecutor {
    private final PetFinderManager petFinder;

    /**
     * Constructs a new {@code PetFinderCommands} with the given PetFinderManager.
     *
     * @param petFinder the manager responsible for executing pet search logic
     */
    public PetFinderCommands(PetFinderManager petFinder) {
        this.petFinder = petFinder;
    }

    /**
     * Handles incoming commands registered to this executor.
     * Supported commands are {@code findpets} and {@code findpetsinarea}.
     *
     * <p>The method routes the command based on its name, invoking the appropriate handler.
     * If the command matches one of the supported names, it returns {@code true},
     * indicating that the command was processed.
     *
     * @param sender the source of the command
     * @param command the command that was executed
     * @param label the alias of the command which was used
     * @param args the arguments passed to the command
     * @return {@code true} if the command was recognized and handled, {@code false} otherwise
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("findpets")) {
            handleFindPets(sender, args);
            return true;
        }

        if (command.getName().equalsIgnoreCase("findpetsinarea")) {
            handleAreaSearch(sender, args);
            return true;
        }

        return false;
    }

    /**
     * Handles the {@code /findpets} command, which initiates a pet search for a single player.
     *
     * <p>The method performs the following steps:
     * <ol>
     *   <li>Attempts to resolve the target player's UUID via {@link #parseTargetUUID}.</li>
     *   <li>If resolution fails (permissions or player not found), the method returns early.</li>
     *   <li>Invokes {@link PetFinderManager#startSearch} with the sender, target UUID, and no area.</li>
     * </ol>
     *
     * <p>This command can target another player only if the sender has {@code petfinder.admin} permission;
     * otherwise, it defaults to the invoking player.
     *
     * @param sender the source of the command (player or console)
     * @param args optional array where {@code args[0]} may be a player name to target
     */
    private void handleFindPets(CommandSender sender, String[] args) {
        UUID targetUUID = parseTargetUUID(sender, args);
        if (targetUUID == null) return;
        petFinder.startSearch(sender, targetUUID, null);
    }

    /**
     * Handles the {@code /findpetsinarea} command, which searches for pets within a specified bounding box.
     *
     * <p>The method performs the following steps:
     * <ol>
     *   <li>Checks that the sender has {@code petfinder.admin} permission; if not, sends an error message.</li>
     *   <li>Verifies that at least six coordinate arguments are provided; if not, sends usage instructions.</li>
     *   <li>Parses the six coordinates into a {@link BoundingBox} via {@link #parseBoundingBox}.</li>
     *   <li>Optionally resolves a target player UUID from additional args (admin only).</li>
     *   <li>If running from console without a target player, prompts to specify a player.</li>
     *   <li>Invokes {@link PetFinderManager#startSearch} with the sender, target UUID, and area.</li>
     * </ol>
     *
     * <p>Usage: {@code /findpetsinarea <x1> <y1> <z1> <x2> <y2> <z2> [player]}
     *
     * @param sender the source of the command (player or console)
     * @param args array containing six coordinates and optional player name
     */
    private void handleAreaSearch(CommandSender sender, String[] args) {
        if (!sender.hasPermission("petfinder.admin")) {
            sender.sendMessage("§cYou need admin permission for area searches.");
            return;
        }

        if (args.length < 6) {
            sender.sendMessage("§cUsage: /findpetsinarea <x1> <y1> <z1> <x2> <y2> <z2> [player]");
            return;
        }

        try {
            BoundingBox area = parseBoundingBox(args);
            UUID targetUUID = args.length > 6 ? parseTargetUUID(sender, Arrays.copyOfRange(args, 6, args.length)) : null;

            if (targetUUID == null && !(sender instanceof Player)) {
                sender.sendMessage("§cSpecify a player for console area searches.");
                return;
            }

            petFinder.startSearch(sender, targetUUID, area);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid coordinates format!");
        }
    }

    /**
     * Parses six string arguments into a {@link BoundingBox}.
     *
     * <p>The method expects coordinates in the order: {@code x1, y1, z1, x2, y2, z2}.
     * It normalizes the values so that minima and maxima are assigned correctly.
     *
     * @param args array of at least six strings representing double values
     * @return a {@link BoundingBox} defined by the parsed coordinates
     * @throws NumberFormatException if any coordinate cannot be parsed as a double
     */
    private BoundingBox parseBoundingBox(String[] args) throws NumberFormatException {
        double x1 = Double.parseDouble(args[0]);
        double y1 = Double.parseDouble(args[1]);
        double z1 = Double.parseDouble(args[2]);
        double x2 = Double.parseDouble(args[3]);
        double y2 = Double.parseDouble(args[4]);
        double z2 = Double.parseDouble(args[5]);

        return new BoundingBox(
                Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2),
                Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2)
        );
    }

    /**
     * Resolves a target player's UUID based on provided arguments or the sender.
     *
     * <p>The method performs the following steps:
     * <ol>
     *   <li>If {@code args} contains a player name and the sender has {@code petfinder.admin} permission,
     *       attempts to locate the online player by name.</li>
     *   <li>If the player is not found or the sender lacks permission, sends an error message.
     *   </li>
     *   <li>If no arguments are provided and the sender is a player, returns the sender's UUID.</li>
     *   <li>If no arguments and the sender is console, prompts to specify a player.</li>
     * </ol>
     *
     * @param sender the source of the command (player or console)
     * @param args optional array where {@code args[0]} may be a player name
     * @return the UUID of the intended target player, or {@code null} if resolution failed
     */
    private UUID parseTargetUUID(CommandSender sender, String[] args) {
        if (args.length > 0) {
            if (!sender.hasPermission("petfinder.admin")) {
                sender.sendMessage("§cYou don't have permission to specify players.");
                return null;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found.");
                return null;
            }
            return target.getUniqueId();
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("§cConsole must specify a player.");
            return null;
        }
        return ((Player) sender).getUniqueId();
    }
}
