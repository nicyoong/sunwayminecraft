package com.sunwayMinecraft.worldtravel;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

public class MiningWorldListener implements Listener {

    private final WorldTravelManager worldTravelManager;

    public MiningWorldListener(WorldTravelManager worldTravelManager) {
        this.worldTravelManager = worldTravelManager;
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();

        if (worldTravelManager.isMiningWorld(player)) {
            player.sendMessage(
                    Component.text("You are now in the Mining World.", NamedTextColor.GOLD));
            player.sendMessage(
                    Component.text("This world is for gathering and exploration, not permanent settlement.",
                            NamedTextColor.YELLOW));

            if (worldTravelManager.isMiningResetPending()) {
                player.sendMessage(
                        Component.text("This Mining World is currently marked as reset pending.",
                                NamedTextColor.GOLD));
            }

            return;
        }

        if (worldTravelManager.isLifeWorld(player)) {
            player.sendMessage(
                    Component.text("You are now in the Life World.", NamedTextColor.GREEN));
        }
    }
}