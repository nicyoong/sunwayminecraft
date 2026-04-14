package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.worldtravel.MiningWorldEvacuationManager;
import com.sunwayMinecraft.worldtravel.MiningWorldState;
import com.sunwayMinecraft.worldtravel.WorldTravelManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class MiningWorldAdminCommands implements CommandExecutor {

    private final WorldTravelManager worldTravelManager;
    private final MiningWorldEvacuationManager evacuationManager;

    public MiningWorldAdminCommands(
            WorldTravelManager worldTravelManager,
            MiningWorldEvacuationManager evacuationManager) {
        this.worldTravelManager = worldTravelManager;
        this.evacuationManager = evacuationManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String commandName = command.getName().toLowerCase();

        switch (commandName) {
            case "miningopen":
                if (evacuationManager.isEvacuationRunning()) {
                    sender.sendMessage(
                            Component.text("Cancel the active evacuation first.", NamedTextColor.RED));
                    return true;
                }

                worldTravelManager.setMiningWorldState(MiningWorldState.OPEN);
                sender.sendMessage(
                        Component.text("Mining World state set to OPEN.", NamedTextColor.GREEN));
                worldTravelManager.broadcastMiningStateChange(
                        Component.text("[Mining World] ", NamedTextColor.DARK_GRAY)
                                .append(Component.text("The Mining World is now OPEN.", NamedTextColor.GREEN)));
                return true;

            case "miningresetpending":
                if (evacuationManager.isEvacuationRunning()) {
                    sender.sendMessage(
                            Component.text("An evacuation is already running.", NamedTextColor.RED));
                    return true;
                }

                worldTravelManager.setMiningWorldState(MiningWorldState.RESET_PENDING);
                sender.sendMessage(
                        Component.text("Mining World state set to RESET_PENDING.", NamedTextColor.GOLD));
                worldTravelManager.broadcastMiningStateChange(
                        Component.text("[Mining World] ", NamedTextColor.DARK_GRAY)
                                .append(Component.text("The Mining World has been marked as RESET PENDING.", NamedTextColor.GOLD)));
                return true;

            case "mininglock":
                if (evacuationManager.isEvacuationRunning()) {
                    sender.sendMessage(
                            Component.text("Cancel the active evacuation first.", NamedTextColor.RED));
                    return true;
                }

                worldTravelManager.setMiningWorldState(MiningWorldState.LOCKED);
                sender.sendMessage(
                        Component.text("Mining World state set to LOCKED.", NamedTextColor.RED));
                worldTravelManager.broadcastMiningStateChange(
                        Component.text("[Mining World] ", NamedTextColor.DARK_GRAY)
                                .append(Component.text("The Mining World is now LOCKED.", NamedTextColor.RED)));
                return true;

            case "miningevacuate":
                return handleEvacuation(sender, args);

            case "miningevaccancel":
                if (!evacuationManager.cancelEvacuation()) {
                    sender.sendMessage(
                            Component.text("There is no active evacuation to cancel.", NamedTextColor.RED));
                    return true;
                }

                sender.sendMessage(
                        Component.text("Mining World evacuation cancelled.", NamedTextColor.GREEN));
                return true;

            case "miningstate":
                sender.sendMessage(
                        Component.text("Current Mining World state: ", NamedTextColor.YELLOW)
                                .append(worldTravelManager.getStateDisplayComponent()));

                if (evacuationManager.isEvacuationRunning()) {
                    sender.sendMessage(
                            Component.text("Evacuation countdown: ", NamedTextColor.YELLOW)
                                    .append(Component.text(
                                            evacuationManager.getSecondsRemaining() + " seconds",
                                            NamedTextColor.GOLD)));
                }

                return true;

            default:
                return false;
        }
    }

    private boolean handleEvacuation(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(
                    Component.text("Usage: /miningevacuate <seconds>", NamedTextColor.RED));
            return true;
        }

        int durationSeconds;
        try {
            durationSeconds = Integer.parseInt(args[0]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(
                    Component.text("Seconds must be a whole number.", NamedTextColor.RED));
            return true;
        }

        if (durationSeconds < 5) {
            sender.sendMessage(
                    Component.text("Evacuation time must be at least 5 seconds.", NamedTextColor.RED));
            return true;
        }

        if (!evacuationManager.startEvacuation(durationSeconds)) {
            sender.sendMessage(
                    Component.text("Could not start evacuation. It may already be running or the world is unavailable.",
                            NamedTextColor.RED));
            return true;
        }

        sender.sendMessage(
                Component.text(
                        "Mining World evacuation started for " + durationSeconds + " seconds.",
                        NamedTextColor.GOLD));
        return true;
    }
}