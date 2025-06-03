package com.sunwayMinecraft.coinflip;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class ItemCoinFlipSystem {
  private final CoinFlipSystem coinFlipSystem;

  public ItemCoinFlipSystem(CoinFlipSystem coinFlipSystem) {
    this.coinFlipSystem = coinFlipSystem;
  }

  public void processItemFlip(Player player, int amount, boolean guessHeads) {
    ItemStack handItem = player.getInventory().getItemInMainHand();

    // Check if player is holding a valid item
    if (handItem == null || handItem.getType().isAir()) {
      player.sendMessage("§cYou must hold an item in your hand!");
      return;
    }

    // Create template BEFORE removing items
    ItemStack template = createTemplate(handItem);

    // Count items in entire inventory
    int available = countAvailableItems(player, template);
    if (available < 1) {
      player.sendMessage("§cYou don't have any of this item!");
      return;
    }

    // Validate bet amount
    if (amount < 1) {
      player.sendMessage("§cInvalid bet amount! Must be at least 1.");
      return;
    }

    // Calculate actual bet amount
    int maxBet = Math.min(available, template.getMaxStackSize());
    if (amount > available) {
      player.sendMessage("§cYou only have " + available + " of that item!");
      return;
    }

    if (amount > maxBet) {
      player.sendMessage("§cMaximum bet is " + maxBet + " (1 stack)!");
      return;
    }

    // Remove items
    removeItems(player, template, amount);

    // Process flip
    boolean won = coinFlipSystem.processFlipLogic(guessHeads);

    // Handle winnings
    if (won) {
      giveWinnings(player, template, amount * 2);
    }

    // Send result
    sendResult(player, won, amount, template);
  }

  private ItemStack createTemplate(ItemStack item) {
    // Create a clean template with amount 1
    ItemStack template = item.clone();
    template.setAmount(1);
    return template;
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

        // Clear slot if amount reaches zero
        if (item.getAmount() == 0) {
          inv.setItem(i, null);
        }
      }
    }
  }

  private void giveWinnings(Player player, ItemStack template, int amount) {
    // Distribute winnings in stacks
    while (amount > 0) {
      int stackSize = Math.min(amount, template.getMaxStackSize());
      ItemStack stack = template.clone();
      stack.setAmount(stackSize);

      // Add to inventory or drop if full
      if (player.getInventory().addItem(stack).isEmpty()) {
        amount -= stackSize;
      } else {
        player.getWorld().dropItem(player.getLocation(), stack);
        amount -= stackSize;
      }
    }
  }

  private void sendResult(Player player, boolean won, int amount, ItemStack item) {
    if (coinFlipSystem.isMuted(player)) return;

    String itemName = item.getType().toString().toLowerCase();
    String result = won ?
            "§aYou won §ex" + (amount * 2) + " " + itemName :
            "§cYou lost §ex" + amount + " " + itemName;

    player.sendMessage(result);
  }
}