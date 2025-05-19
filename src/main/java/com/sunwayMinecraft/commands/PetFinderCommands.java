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

}
