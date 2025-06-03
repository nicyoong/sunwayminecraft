package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.coinflip.CoinFlipSystem;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CoinFlipCommands implements CommandExecutor, TabCompleter {
    private final CoinFlipSystem coinFlipSystem;

    public CoinFlipCommands(CoinFlipSystem coinFlipSystem) {
        this.coinFlipSystem = coinFlipSystem;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }