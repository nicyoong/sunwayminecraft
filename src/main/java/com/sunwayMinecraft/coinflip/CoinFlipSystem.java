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