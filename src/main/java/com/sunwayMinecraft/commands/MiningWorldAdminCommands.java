package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.worldtravel.MiningWorldState;
import com.sunwayMinecraft.worldtravel.WorldTravelManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class MiningWorldAdminCommands implements CommandExecutor {

    private final WorldTravelManager worldTravelManager;

    public MiningWorldAdminCommands(WorldTravelManager worldTravelManager) {
        this.worldTravelManager = worldTravelManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String commandName = command.getName().toLowerCase();

        switch (commandName) {
            case "miningopen":
                worldTravelManager.setMiningWorldState(MiningWorldState.OPEN);
                sender.sendMessage(
                        Component.text("Mining World state set to OPEN.", NamedTextColor.GREEN));
                worldTravelManager.broadcastMiningStateChange(
                        Component.text("[Mining World] ", NamedTextColor.DARK_GRAY)
                                .append(Component.text("The Mining World is now OPEN.", NamedTextColor.GREEN)));
                return true;

            case "miningresetpending":
                worldTravelManager.setMiningWorldState(MiningWorldState.RESET_PENDING);
                sender.sendMessage(
                        Component.text("Mining World state set to RESET_PENDING.", NamedTextColor.GOLD));
                worldTravelManager.broadcastMiningStateChange(
                        Component.text("[Mining World] ", NamedTextColor.DARK_GRAY)
                                .append(Component.text("The Mining World has been marked as RESET PENDING.", NamedTextColor.GOLD)));
                return true;

            case "mininglock":
                worldTravelManager.setMiningWorldState(MiningWorldState.LOCKED);
                sender.sendMessage(
                        Component.text("Mining World state set to LOCKED.", NamedTextColor.RED));
                worldTravelManager.broadcastMiningStateChange(
                        Component.text("[Mining World] ", NamedTextColor.DARK_GRAY)
                                .append(Component.text("The Mining World is now LOCKED.", NamedTextColor.RED)));
                return true;

            default:
                return false;
        }
    }
}