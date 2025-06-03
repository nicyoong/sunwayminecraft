package com.sunwayMinecraft.coinflip;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class CoinFlipSystem {
    private final Economy econ;
    private final Set<UUID> mutedPlayers = new HashSet<>();
    private final Random random = new Random();

    public CoinFlipSystem(Economy econ) {
        this.econ = econ;
    }

    public void processCoinFlip(Player player, double amount, boolean playerGuessHeads) {
        if (econ.getBalance(player) < amount) {
            player.sendMessage(ChatColor.RED + "Insufficient funds!");
            return;
        }

        // Deduct bet amount
        econ.withdrawPlayer(player, amount);

        // Flip coin (true = heads, false = tails)
        boolean isHeads = random.nextBoolean();
        boolean won = (playerGuessHeads == isHeads);

        // Handle winnings
        if (won) econ.depositPlayer(player, amount * 2);

        // Send result if not muted
        if (!mutedPlayers.contains(player.getUniqueId())) {
            String result = isHeads ? "Heads" : "Tails";
            ChatColor color = won ? ChatColor.GREEN : ChatColor.RED;
            String outcome = won ? "won " + econ.format(amount) : "lost " + econ.format(amount);
            player.sendMessage(color + "Coin landed on " + result + "! You " + outcome + ".");
        }
    }
    