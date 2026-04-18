package com.sunwayMinecraft.residency.listener;

import com.sunwayMinecraft.residency.admin.AdminSelectionManager;
import com.sunwayMinecraft.residency.util.Message;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class UnitRegistrationToolListener implements Listener {
    private final AdminSelectionManager selectionManager;

    public UnitRegistrationToolListener(AdminSelectionManager selectionManager) {
        this.selectionManager = selectionManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("sunway.residency.admin")) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!AdminSelectionManager.isWand(player.getInventory().getItemInMainHand())) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            selectionManager.setPos1(player.getUniqueId(), clicked.getLocation());
            player.sendMessage(Message.ok("pos1 set to "
                    + clicked.getWorld().getName() + " "
                    + clicked.getX() + "," + clicked.getY() + "," + clicked.getZ()));
            event.setCancelled(true);
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            selectionManager.setPos2(player.getUniqueId(), clicked.getLocation());
            player.sendMessage(Message.ok("pos2 set to "
                    + clicked.getWorld().getName() + " "
                    + clicked.getX() + "," + clicked.getY() + "," + clicked.getZ()));
            event.setCancelled(true);
        }
    }
}
