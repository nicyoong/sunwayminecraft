package com.sunwayMinecraft.residency.listener;

import com.sunwayMinecraft.residency.ResidencyManager;
import com.sunwayMinecraft.residency.access.AccessDecision;
import com.sunwayMinecraft.residency.access.PremisesAccessService;
import com.sunwayMinecraft.residency.domain.ActionType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResidencyBlockListenerTest {
    private ServerMock server;
    private PlayerMock player;
    private ResidencyManager manager;
    private PremisesAccessService access;
    private Block block;
    private Location location;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        player = server.addPlayer();
        manager = mock(ResidencyManager.class);
        access = mock(PremisesAccessService.class);
        block = mock(Block.class);
        location = player.getLocation();
        when(block.getLocation()).thenReturn(location);
        when(manager.getAccessService()).thenReturn(access);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void managedDeniedBlockBreakIsCancelledAndUnmanagedBreakBypassesAccessService() {
        ResidencyBlockListener listener = new ResidencyBlockListener(manager);
        BlockBreakEvent managed = mock(BlockBreakEvent.class);
        when(managed.getBlock()).thenReturn(block);
        when(managed.getPlayer()).thenReturn(player);
        when(manager.isManagedLocation(location)).thenReturn(true);
        when(access.check(player, location, ActionType.BREAK_BLOCK)).thenReturn(AccessDecision.deny(null, null, "Denied"));

        listener.onBreak(managed);
        verify(managed).setCancelled(true);

        BlockBreakEvent unmanaged = mock(BlockBreakEvent.class);
        when(unmanaged.getBlock()).thenReturn(block);
        when(manager.isManagedLocation(location)).thenReturn(false);
        listener.onBreak(unmanaged);
        verify(unmanaged, never()).setCancelled(true);
    }

    @Test
    void containerInteractionUsesContainerActionAndCancelsDenial() {
        ResidencyBlockListener listener = new ResidencyBlockListener(manager);
        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        when(event.getClickedBlock()).thenReturn(block);
        when(event.getPlayer()).thenReturn(player);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        when(block.getType()).thenReturn(Material.CHEST);
        when(block.getBlockData()).thenReturn(null);
        when(manager.isManagedLocation(location)).thenReturn(true);
        when(access.check(player, location, ActionType.USE_CONTAINER)).thenReturn(AccessDecision.deny(null, null, "No containers"));

        listener.onInteract(event);

        verify(access).check(player, location, ActionType.USE_CONTAINER);
        verify(event).setCancelled(true);
    }
}
