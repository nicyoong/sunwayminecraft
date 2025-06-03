package com.sunwayMinecraft.coinflip;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;
import java.util.Map;

public class ItemCoinFlipSystem {
    private final CoinFlipSystem coinFlipSystem;

    public ItemCoinFlipSystem(CoinFlipSystem coinFlipSystem) {
        this.coinFlipSystem = coinFlipSystem;
    }