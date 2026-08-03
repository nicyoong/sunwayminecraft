package com.sunwayMinecraft.residency.listener;

import com.sunwayMinecraft.residency.admin.AdminSelectionManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UnitRegistrationToolListenerTest {
    @Test
    void adminWandClicksSetTheCorrectCornerAndCancelTheInteraction() {
        AdminSelectionManager selection = mock(AdminSelectionManager.class);
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        ItemStack wand = mock(ItemStack.class);
        UUID id = UUID.randomUUID();
        Block block = mock(Block.class);
        World world = mock(World.class);
        Location location = new Location(world, 2, 64, 3);
        when(player.hasPermission("sunway.residency.admin")).thenReturn(true);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getItemInMainHand()).thenReturn(wand);
        when(player.getUniqueId()).thenReturn(id);
        when(selection.isWand(wand)).thenReturn(true);
        when(block.getLocation()).thenReturn(location);
        when(block.getWorld()).thenReturn(world);
        when(world.getName()).thenReturn("world");
        when(block.getX()).thenReturn(2);
        when(block.getY()).thenReturn(64);
        when(block.getZ()).thenReturn(3);
        UnitRegistrationToolListener listener = new UnitRegistrationToolListener(selection);

        PlayerInteractEvent left = mock(PlayerInteractEvent.class);
        when(left.getPlayer()).thenReturn(player);
        when(left.getHand()).thenReturn(EquipmentSlot.HAND);
        when(left.getClickedBlock()).thenReturn(block);
        when(left.getAction()).thenReturn(Action.LEFT_CLICK_BLOCK);
        listener.onInteract(left);
        verify(selection).setPos1(id, location);
        verify(left).setCancelled(true);

        PlayerInteractEvent right = mock(PlayerInteractEvent.class);
        when(right.getPlayer()).thenReturn(player);
        when(right.getHand()).thenReturn(EquipmentSlot.HAND);
        when(right.getClickedBlock()).thenReturn(block);
        when(right.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        listener.onInteract(right);
        verify(selection).setPos2(id, location);
        verify(right).setCancelled(true);
    }

    @Test
    void nonAdminsAndOffhandInteractionsDoNotChangeSelections() {
        AdminSelectionManager selection = mock(AdminSelectionManager.class);
        Player player = mock(Player.class);
        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(player.hasPermission("sunway.residency.admin")).thenReturn(false);

        new UnitRegistrationToolListener(selection).onInteract(event);

        verify(selection, never()).setPos1(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(selection, never()).setPos2(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
