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

public class PetFinderCommands implements CommandExecutor {
    private final PetFinderManager petFinder;

    public PetFinderCommands(PetFinderManager petFinder) {
        this.petFinder = petFinder;
    }

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

    private void handleFindPets(CommandSender sender, String[] args) {
        UUID targetUUID = parseTargetUUID(sender, args);
        if (targetUUID == null) return;
        petFinder.startSearch(sender, targetUUID, null);
    }

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
