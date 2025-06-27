package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.regions.Region;
import com.sunwayMinecraft.regions.RegionManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class RegionCommands implements CommandExecutor {
    private final RegionManager regionManager;

    public RegionCommands(RegionManager regionManager) {
        this.regionManager = regionManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String commandName = cmd.getName().toLowerCase();

        switch (commandName) {
            case "sunwayregioncreate": return handleCreate(sender, args);
            case "sunwayregionresize": return handleResize(sender, args);
            case "sunwayregiondelete": return handleDelete(sender, args);
            case "sunwayregiondecouple": return handleDecouple(sender, args);
            case "sunwayregiontrust": return handleTrust(sender, args, true);
            case "sunwayregionuntrust": return handleTrust(sender, args, false);
            case "sunwayregiontrustlist": return handleTrustList(sender, args);
            case "sunwayregionlist": return handleList(sender);
            case "sunwayregionhere": return handleHere(sender);
            default:
                sender.sendMessage("§cUnknown region command");
                return true;
        }
    }

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (args.length < 8) {
            sender.sendMessage("§cUsage: /sunwayregioncreate <name> <world> <minX> <minY> <minZ> <maxX> <maxY> <maxZ>");
            return true;
        }

        String name = args[0];
        String worldName = args[1];
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            sender.sendMessage("§cWorld not found: " + worldName);
            return true;
        }

        try {
            int minX = Integer.parseInt(args[2]);
            int minY = Integer.parseInt(args[3]);
            int minZ = Integer.parseInt(args[4]);
            int maxX = Integer.parseInt(args[5]);
            int maxY = Integer.parseInt(args[6]);
            int maxZ = Integer.parseInt(args[7]);

            if (regionManager.createRegion(name, worldName, minX, minY, minZ, maxX, maxY, maxZ, null, false)) {
                sender.sendMessage("§aRegion '" + name + "' created");
            } else {
                sender.sendMessage("§cRegion creation failed (name already exists?)");
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§cCoordinates must be integers");
        }
        return true;
    }

    private boolean handleResize(CommandSender sender, String[] args) {
        if (args.length < 7) {
            sender.sendMessage("§cUsage: /sunwayregionresize <name> <minX> <minY> <minZ> <maxX> <maxY> <maxZ>");
            return true;
        }

        String name = args[0];
        try {
            int minX = Integer.parseInt(args[1]);
            int minY = Integer.parseInt(args[2]);
            int minZ = Integer.parseInt(args[3]);
            int maxX = Integer.parseInt(args[4]);
            int maxY = Integer.parseInt(args[5]);
            int maxZ = Integer.parseInt(args[6]);

            if (regionManager.updateRegionBounds(name, minX, minY, minZ, maxX, maxY, maxZ)) {
                sender.sendMessage("§aRegion '" + name + "' resized");
            } else {
                sender.sendMessage("§cRegion resize failed (not found or decoupled?)");
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§cCoordinates must be integers");
        }
        return true;
    }

    private boolean handleDelete(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§cUsage: /sunwayregiondelete <name>");
            return true;
        }

        String name = args[0];
        if (regionManager.deleteRegion(name)) {
            sender.sendMessage("§aRegion '" + name + "' deleted");
        } else {
            sender.sendMessage("§cRegion not found: " + name);
        }
        return true;
    }

    private boolean handleDecouple(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§cUsage: /sunwayregiondecouple <name>");
            return true;
        }

        String name = args[0];
        if (regionManager.setDecoupled(name, true)) {
            sender.sendMessage("§aRegion '" + name + "' decoupled from GP");
        } else {
            sender.sendMessage("§cRegion not found: " + name);
        }
        return true;
    }

    private boolean handleTrust(CommandSender sender, String[] args, boolean add) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /" + (add ? "sunwayregiontrust" : "sunwayregionuntrust") + " <name> <player>");
            return true;
        }

        String name = args[0];
        Player player = Bukkit.getPlayer(args[1]);
        if (player == null) {
            sender.sendMessage("§cPlayer not found: " + args[1]);
            return true;
        }

        if (regionManager.manageTrust(name, player.getUniqueId(), add)) {
            String action = add ? "trusted" : "untrusted";
            sender.sendMessage("§aPlayer " + player.getName() + " " + action + " in region " + name);
        } else {
            sender.sendMessage("§cFailed to modify trust (region not found or not decoupled?)");
        }
        return true;
    }

    private boolean handleTrustList(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§cUsage: /sunwayregiontrustlist <name>");
            return true;
        }

        Region region = regionManager.getRegionByName(args[0]);
        if (region == null) {
            sender.sendMessage("§cRegion not found");
            return true;
        }

        if (!region.isDecoupled()) {
            sender.sendMessage("§cRegion is still coupled to GP");
            return true;
        }

        Set<UUID> trusted = region.getTrustedPlayers();
        if (trusted.isEmpty()) {
            sender.sendMessage("§eNo trusted players for " + region.getName());
            return true;
        }

        sender.sendMessage("§6Trusted players in " + region.getName() + ":");
        for (UUID uuid : trusted) {
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            sender.sendMessage("§7 - " + (name != null ? name : uuid.toString()));
        }
        return true;
    }

    private boolean handleList(CommandSender sender) {
        Map<String, Region> regions = regionManager.getRegions();
        if (regions.isEmpty()) {
            sender.sendMessage("§eNo regions defined");
            return true;
        }

        sender.sendMessage("§6Defined regions (" + regions.size() + "):");
        for (Region region : regions.values()) {
            String status = region.isDecoupled() ? "Decoupled" : "GP-Linked";
            sender.sendMessage("§a - " + region.getName() + "§7 (" + status + ")");
        }
        return true;
    }

    private boolean handleHere(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command");
            return true;
        }

        Player player = (Player) sender;
        Location loc = player.getLocation();

        // Null-safe world check
        if (loc.getWorld() == null) {
            sender.sendMessage("§cInvalid world location");
            return true;
        }

        List<Region> regions = regionManager.getRegionsAt(loc);
        if (regions.isEmpty()) {
            sender.sendMessage("§eNo regions at your location");
            return true;
        }

        sender.sendMessage("§6Regions at your location:");
        for (Region region : regions) {
            String status = region.isDecoupled() ? "Decoupled" : "GP-Linked";
            sender.sendMessage("§a - " + region.getName() + "§7 (" + status + ")");
        }
        return true;
    }
}