package com.sunwayMinecraft.coinflip;

import java.util.UUID;

public class PlayerStats {
    private final UUID uuid;
    private int moneyWins;
    private int moneyLosses;
    private double moneyWagered;
    private double moneyWon;
    private int itemWins;
    private int itemLosses;
    private int itemsWagered;
    private int itemsWon;

    public PlayerStats(UUID uuid) {
        this.uuid = uuid;
    }
}