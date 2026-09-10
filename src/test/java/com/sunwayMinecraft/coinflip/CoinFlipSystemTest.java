package com.sunwayMinecraft.coinflip;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.milkbowl.vault.economy.Economy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CoinFlipSystemTest {
    private ServerMock server;
    private Economy econ;
    private CoinFlipDatabase database;
    private CoinFlipSystem coinFlipSystem;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        econ = mock(Economy.class);
        database = mock(CoinFlipDatabase.class);
        coinFlipSystem = spy(new CoinFlipSystem(econ, database));
        player = server.addPlayer();

        when(database.getPlayerStats(any(UUID.class))).thenAnswer(invocation -> new PlayerStats(invocation.getArgument(0)));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static Component legacy(String text) {
        return LegacyComponentSerializer.legacySection().deserialize(text);
    }

    // MockBukkit deprecated both assertSaid overloads mid-migration to its matcher API;
    // this keeps the same consume-and-compare semantics on the supported primitive.
    private void assertSaid(Component expected) {
        Component actual = player.nextComponentMessage();
        assertNotNull(actual, "No more messages were sent");
        assertEquals(expected, actual);
    }

    @Test
    void testProcessCoinFlipWin() {
        double amount = 100.0;
        when(econ.getBalance(player)).thenReturn(200.0);
        when(econ.format(amount)).thenReturn("$100.00");
        doReturn(true).when(coinFlipSystem).processFlipLogic(anyBoolean());

        coinFlipSystem.processCoinFlip(player, amount, true);

        verify(econ).withdrawPlayer(player, amount);
        verify(econ, times(1)).depositPlayer(player, amount * 2);
        verify(database).updateStats(any(PlayerStats.class));
        player.nextMessage(); // Consume "You bet..." message
        assertSaid(legacy("§aYou won §e$100.00"));
    }

    @Test
    void testProcessCoinFlipLoss() {
        double amount = 100.0;
        when(econ.getBalance(player)).thenReturn(200.0);
        when(econ.format(amount)).thenReturn("$100.00");
        doReturn(false).when(coinFlipSystem).processFlipLogic(anyBoolean());

        coinFlipSystem.processCoinFlip(player, amount, true);

        verify(econ).withdrawPlayer(player, amount);
        verify(econ, never()).depositPlayer(eq(player), anyDouble());
        verify(database).updateStats(any(PlayerStats.class));
        player.nextMessage(); // Consume "You bet..." message
        assertSaid(legacy("§cYou lost §e$100.00"));
    }

    @Test
    void testProcessCoinFlipInsufficientFunds() {
        double amount = 100.0;
        when(econ.getBalance(player)).thenReturn(50.0);

        coinFlipSystem.processCoinFlip(player, amount, true);

        verify(econ, never()).withdrawPlayer(any(org.bukkit.OfflinePlayer.class), anyDouble());
        assertSaid(legacy("§cInsufficient funds!"));
    }

    @Test
    void testHandleMute() {
        assertFalse(coinFlipSystem.isMuted(player));

        coinFlipSystem.handleMute(player, true);
        assertTrue(coinFlipSystem.isMuted(player));
        assertSaid(legacy("§6Coin flip messages muted."));

        coinFlipSystem.handleMute(player, false);
        assertFalse(coinFlipSystem.isMuted(player));
        assertSaid(legacy("§6Coin flip messages unmuted."));
    }

    @Test
    void testMutedResult() {
        double amount = 100.0;
        when(econ.getBalance(player)).thenReturn(200.0);
        when(econ.format(amount)).thenReturn("$100.00");
        doReturn(true).when(coinFlipSystem).processFlipLogic(anyBoolean());

        coinFlipSystem.handleMute(player, true);
        // Consume the mute message
        player.nextMessage();

        coinFlipSystem.processCoinFlip(player, amount, true);

        // Should have said the bet message
        assertSaid(legacy("§aYou bet heads with $100.00"));
        // But NOT the result message
        assertNull(player.nextMessage());
    }
}
