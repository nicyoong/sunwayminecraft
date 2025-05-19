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
}
