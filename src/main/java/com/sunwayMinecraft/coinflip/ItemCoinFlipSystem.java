package com.sunwayMinecraft.coinflip;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public class ItemCoinFlipSystem {
  private final CoinFlipSystem coinFlipSystem;
  private final CoinFlipDatabase database;

  public ItemCoinFlipSystem(CoinFlipSystem coinFlipSystem, CoinFlipDatabase database) {
    this.coinFlipSystem = coinFlipSystem;
    this.database = database;
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

    // Prevent non-stackable items from being bet
    if (!isStackableItem(template)) {
      player.sendMessage("§cYou cannot bet non-stackable items like tools or weapons!");
      return;
    }

    player.sendMessage(
        "§aYou bet "
            + (guessHeads ? "heads" : "tails")
            + " with "
            + amount
            + " "
            + template.getType().toString().toLowerCase());

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

    PlayerStats stats = database.getPlayerStats(player.getUniqueId());
    if (won) {
      stats.addItemWin(amount);
    } else {
      stats.addItemLoss(amount);
    }
    database.updateStats(stats);

    // Handle winnings
    if (won) {
      giveWinnings(player, template, amount * 2);
    }

    // Send result
    sendResult(player, won, amount, template);
  }

  private boolean isStackableItem(ItemStack item) {
    // Check max stack size
    if (item.getMaxStackSize() == 1) {
      return false;
    }

    // Check for specific non-stackable types
    Material type = item.getType();
    if (isTool(type) || isWeapon(type) || isArmor(type) || isSpecialItem(type)) {
      return false;
    }

    // Check for custom items with durability
    if (item.hasItemMeta() && item.getItemMeta().hasEnchants()) {
      return false;
    }

    return true;
  }

  private boolean isTool(Material type) {
    return type.name().endsWith("_AXE")
        || type.name().endsWith("_PICKAXE")
        || type.name().endsWith("_SHOVEL")
        || type.name().endsWith("_HOE")
        || type == Material.FISHING_ROD
        || type == Material.SHEARS
        || type == Material.FLINT_AND_STEEL;
  }

  private boolean isWeapon(Material type) {
    return type.name().endsWith("_SWORD")
        || type == Material.BOW
        || type == Material.CROSSBOW
        || type == Material.TRIDENT;
  }

  private boolean isArmor(Material type) {
    return type.name().endsWith("_HELMET")
        || type.name().endsWith("_CHESTPLATE")
        || type.name().endsWith("_LEGGINGS")
        || type.name().endsWith("_BOOTS");
  }

  private boolean isSpecialItem(Material type) {
    return type == Material.ELYTRA
        || type == Material.SHIELD
        || type == Material.TOTEM_OF_UNDYING
        || type == Material.COMPASS
        || type == Material.CLOCK
        || type == Material.BUNDLE;
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

    String itemName = item.getType().toString().toLowerCase().replace("_", " ");
    String result =
        won
            ? "§aYou won §ex" + (amount * 2) + " " + itemName
            : "§cYou lost §ex" + amount + " " + itemName;

    player.sendMessage(result);
  }
}
