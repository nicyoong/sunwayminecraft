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

    public void processItemFlip(Player player, int amount, boolean guessHeads) {
        ItemStack handItem = player.getInventory().getItemInMainHand();

        if (handItem == null || handItem.getType().isAir()) {
            player.sendMessage("§cYou must hold an item in your hand!");
            return;
        }

        int available = countAvailableItems(player, handItem);
        if (available == 0) {
            player.sendMessage("§cYou don't have any of this item!");
            return;
        }

        int betAmount = Math.min(amount, Math.min(available, handItem.getMaxStackSize()));
        if (betAmount < 1) {
            player.sendMessage("§cInvalid bet amount!");
            return;
        }

        // Remove items
        removeItems(player, handItem, betAmount);

        // Process flip
        boolean won = coinFlipSystem.processFlipLogic(guessHeads);

        // Handle winnings
        if (won) {
            giveWinnings(player, handItem, betAmount * 2);
        }

        // Send result
        sendResult(player, won, betAmount, handItem);
    }

    private int countAvailableItems(Player player, ItemStack template) {
        PlayerInventory inv = player.getInventory();
        int count = 0;

        for (ItemStack item : inv.getStorageContents()) {
            if (item != null && item.isSimilar(template)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private void removeItems(Player player, ItemStack template, int amount) {
        PlayerInventory inv = player.getInventory();
        int toRemove = amount;

        for (int i = 0; i < inv.getSize() && toRemove > 0; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.isSimilar(template)) {
                int remove = Math.min(item.getAmount(), toRemove);
                item.setAmount(item.getAmount() - remove);
                toRemove -= remove;
            }
        }
    }

    private void giveWinnings(Player player, ItemStack template, int amount) {
        ItemStack reward = template.clone();

        while (amount > 0) {
            int stackSize = Math.min(amount, template.getMaxStackSize());
            reward.setAmount(stackSize);
            player.getInventory().addItem(reward);
            amount -= stackSize;
        }
    }