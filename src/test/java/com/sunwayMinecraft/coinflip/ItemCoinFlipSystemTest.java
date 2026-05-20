package com.sunwayMinecraft.coinflip;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ItemCoinFlipSystemTest {
    private ServerMock server;
    private CoinFlipSystem coinFlipSystem;
    private CoinFlipDatabase database;
    private ItemCoinFlipSystem itemCoinFlipSystem;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        coinFlipSystem = mock(CoinFlipSystem.class);
        database = mock(CoinFlipDatabase.class);
        itemCoinFlipSystem = new ItemCoinFlipSystem(coinFlipSystem, database);
        player = server.addPlayer();

        when(database.getPlayerStats(any(UUID.class))).thenAnswer(invocation -> new PlayerStats(invocation.getArgument(0)));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void testProcessItemFlipSuccessWin() {
        ItemStack diamonds = new ItemStack(Material.DIAMOND, 10);
        player.getInventory().setItemInMainHand(diamonds);

        when(coinFlipSystem.processFlipLogic(anyBoolean())).thenReturn(true);

        itemCoinFlipSystem.processItemFlip(player, 5, true);

        // Remaining 5 in hand + 10 winnings = 15 total
        int totalDiamonds = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.DIAMOND) {
                totalDiamonds += item.getAmount();
            }
        }
        assertEquals(15, totalDiamonds);
        player.nextMessage(); // Consume "You bet..." message
        player.assertSaid("§aYou won §ex10 diamond");
        verify(database).updateStats(any(PlayerStats.class));
    }

    @Test
    void testProcessItemFlipSuccessLoss() {
        ItemStack diamonds = new ItemStack(Material.DIAMOND, 10);
        player.getInventory().setItemInMainHand(diamonds);

        when(coinFlipSystem.processFlipLogic(anyBoolean())).thenReturn(false);

        itemCoinFlipSystem.processItemFlip(player, 5, true);

        assertEquals(5, player.getInventory().getItemInMainHand().getAmount());
        player.nextMessage(); // Consume "You bet..." message
        player.assertSaid("§cYou lost §ex5 diamond");
        verify(database).updateStats(any(PlayerStats.class));
    }

    @Test
    void testProcessItemFlipNonStackable() {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        player.getInventory().setItemInMainHand(sword);

        itemCoinFlipSystem.processItemFlip(player, 1, true);

        player.assertSaid("§cYou cannot bet non-stackable items like tools or weapons!");
        verify(coinFlipSystem, never()).processFlipLogic(anyBoolean());
    }

    @Test
    void testProcessItemFlipInsufficientItems() {
        ItemStack diamonds = new ItemStack(Material.DIAMOND, 2);
        player.getInventory().setItemInMainHand(diamonds);

        itemCoinFlipSystem.processItemFlip(player, 5, true);

        player.nextMessage(); // Consume "You bet..." message
        player.assertSaid("§cYou only have 2 of that item!");
        verify(coinFlipSystem, never()).processFlipLogic(anyBoolean());
    }

    @Test
    void testProcessItemFlipInvalidAmount() {
        ItemStack diamonds = new ItemStack(Material.DIAMOND, 10);
        player.getInventory().setItemInMainHand(diamonds);

        itemCoinFlipSystem.processItemFlip(player, 0, true);

        player.nextMessage(); // Consume "You bet..." message
        player.assertSaid("§cInvalid bet amount! Must be at least 1.");
        verify(coinFlipSystem, never()).processFlipLogic(anyBoolean());
    }

    @Test
    void testProcessItemFlipMaxBet() {
        ItemStack diamonds = new ItemStack(Material.DIAMOND, 100);
        player.getInventory().setItemInMainHand(diamonds);

        // Max bet is 1 stack (64)
        itemCoinFlipSystem.processItemFlip(player, 65, true);

        player.nextMessage(); // Consume "You bet..." message
        player.assertSaid("§cMaximum bet is 64 (1 stack)!");
        verify(coinFlipSystem, never()).processFlipLogic(anyBoolean());
    }

    @Test
    void testProcessItemFlipNoItemInHand() {
        player.getInventory().setItemInMainHand(null);

        itemCoinFlipSystem.processItemFlip(player, 5, true);

        player.assertSaid("§cYou must hold an item in your hand!");
    }
}
