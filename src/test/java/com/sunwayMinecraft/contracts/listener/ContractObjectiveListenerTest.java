package com.sunwayMinecraft.contracts.listener;

import com.sunwayMinecraft.contracts.service.ContractObjectiveService;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class ContractObjectiveListenerTest {
    @Test
    void rightClickingABlockRecordsTheInteractionAtThatBlock() {
        ContractObjectiveService objectives = mock(ContractObjectiveService.class);
        ContractObjectiveListener listener = new ContractObjectiveListener(objectives);
        Player player = mock(Player.class);
        Block block = mock(Block.class);
        Location location = mock(Location.class);
        when(block.getLocation()).thenReturn(location);

        listener.onPlayerInteract(new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, null, block, BlockFace.UP));

        verify(objectives).recordInteraction(player, location);
    }

    @Test
    void airClicksAndCancelledEventsDoNotRecordObjectives() {
        ContractObjectiveService objectives = mock(ContractObjectiveService.class);
        ContractObjectiveListener listener = new ContractObjectiveListener(objectives);
        Player player = mock(Player.class);
        PlayerInteractEvent airClick = new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, null, null, BlockFace.SELF);
        PlayerInteractEvent cancelledBlockClick = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, null, mock(Block.class), BlockFace.UP);
        cancelledBlockClick.setCancelled(true);

        listener.onPlayerInteract(airClick);
        listener.onPlayerInteract(cancelledBlockClick);

        verifyNoInteractions(objectives);
    }
}
