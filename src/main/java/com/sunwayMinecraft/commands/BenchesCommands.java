package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.benches.BenchesConfigManager;
import com.sunwayMinecraft.benches.RegionManager;
import com.sunwayMinecraft.benches.CuboidRegion;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import java.util.List;

/**
 * The BenchesCommands class handles commands related to benches in the Minecraft plugin.
 * It processes several commands such as reloading configurations, listing benches, 
 * providing bench information, and checking the player's current region.
 * 
 * The class implements the `CommandExecutor` interface, allowing it to handle various 
 * commands that players or admins can execute in the game. The commands include:
 * - `/reloadsunwaybenches`: Reloads the bench configurations and regions.
 * - `/listbenches`: Lists all configured bench regions.
 * - `/benchinfo <name>`: Displays information about a specific bench region.
 * - `/checkbenchregion`: Checks if a player is in any configured bench region.
 * 
 * Key functionality includes:
 * - Permission checks to ensure the sender has the required permission to execute each command.
 * - Reloading the bench region configurations from the configuration file.
 * - Listing the names of all configured bench regions.
 * - Displaying detailed information about a specific bench region, such as 
 *   its world and coordinates.
 * - Checking the player's current location to determine if they are in a bench region.
 * 
 * The main methods provided by this class are:
 * - `handleReload()`: Reloads the bench configuration and region data.
 * - `handleListBenches()`: Lists all configured bench regions.
 * - `handleBenchInfo()`: Provides information about a specific bench region.
 * - `handleRegionCheck()`: Checks if a player is inside a bench region and notifies them.
 * - `formatLocation()`: Formats a location into a string for display.
 * 
 * Commands are routed based on the command name, with each case in the switch statement handling 
 * a specific type of action or query related to benches.
 */
public class BenchesCommands implements CommandExecutor {
    private final BenchesConfigManager configManager;
    private final RegionManager regionManager;

    /**
     * Constructs a new BenchesCommands instance with the provided BenchesConfigManager and RegionManager.
     * 
     * This constructor initializes the command handler by setting the configuration manager and the 
     * region manager instances, which are necessary for interacting with bench configurations and 
     * checking bench regions. The `BenchesConfigManager` is used to manage and reload bench configurations, 
     * while the `RegionManager` is responsible for handling cuboid regions for the benches.
     * 
     * @param configManager The BenchesConfigManager instance, used to manage bench configuration data.
     * @param regionManager The RegionManager instance, used to manage and check bench regions in the world.
     */
    public BenchesCommands(BenchesConfigManager configManager, RegionManager regionManager) {
        this.configManager = configManager;
        this.regionManager = regionManager;
    }

    /**
     * Handles the execution of various commands related to benches in the plugin. This method 
     * is called when a command is issued by a player or admin. It processes commands like reloading 
     * configurations, listing bench regions, providing information about specific benches, and checking 
     * a player's current region.
     * 
     * The method performs the following actions:
     * - It checks the name of the command being executed and routes it to the appropriate handler method.
     * - The switch-case structure handles the following commands:
     *   - `/reloadsunwaybenches`: Calls the `handleReload()` method to reload the bench configurations.
     *   - `/listbenches`: Calls the `handleListBenches()` method to list all configured bench regions.
     *   - `/benchinfo <name>`: Calls the `handleBenchInfo()` method to show detailed information 
     *     about a specific bench region.
     *   - `/checkbenchregion`: Calls the `handleRegionCheck()` method to check if a player is in a 
     *     bench region and notify them.
     * 
     * If the command does not match any of the cases, the method returns `false` to indicate that 
     * the command is not recognized.
     * 
     * @param sender The sender of the command (player, console, etc.).
     * @param command The command that was executed.
     * @param label The alias of the command that was used.
     * @param args The arguments provided with the command.
     * @return A boolean indicating whether the command was successfully executed. Returns `true` if 
     *         the command is handled, `false` otherwise.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (command.getName().toLowerCase()) {
            case "reloadsunwaybenches":
                return handleReload(sender);
            case "listbenches":
                return handleListBenches(sender);
            case "benchinfo":
                return handleBenchInfo(sender, args);
            case "checkbenchregion":
                return handleRegionCheck(sender);
            default:
                return false;
        }
    }

    /**
     * Handles the `/reloadsunwaybenches` command, which reloads the bench configurations and regions 
     * from the plugin's configuration file.
     * 
     * The method performs the following steps:
     * 1. Checks if the sender has the required permission (`benches.reload`) to reload the bench configurations. 
     *    If the sender does not have permission, a message is sent indicating that they cannot perform the action.
     * 2. If the sender has permission, the method attempts to reload the bench regions 
     *    using the `regionManager.reloadRegions()` method.
     * 3. If the reload is successful, a success message is sent to the sender indicating 
     *    that the bench configurations were reloaded.
     * 4. If an exception occurs during the reload, an error message is sent to the sender with details 
     *    of the exception.
     * 
     * This command is typically used by administrators to refresh the configuration or to reload newly 
     * added/modified bench regions.
     * 
     * @param sender The sender of the command, which can be a player, console, or other entity.
     * @return A boolean indicating whether the command was successfully handled. Always returns `true` 
     *         since this command has a clear success or failure message.
     */
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("benches.reload")) {
            sender.sendMessage("§cYou don't have permission to reload bench configurations!");
            return true;
        }

        try {
            regionManager.reloadRegions();
            sender.sendMessage("§aSuccessfully reloaded bench configurations!");
            return true;
        } catch (Exception e) {
            sender.sendMessage("§cError reloading bench configs: " + e.getMessage());
            return true;
        }
    }

    /**
     * Handles the `/listbenches` command, which lists all configured bench regions in the plugin.
     * 
     * The method performs the following steps:
     * 1. Checks if the sender has the required permission (`benches.list`) to list the benches. 
     *    If the sender does not have permission, a message is sent indicating that they 
     *    do not have permission to perform this action.
     * 2. If the sender has permission, the method retrieves a list of bench region names 
     *    from the `regionManager`.
     * 3. The list of bench names is then displayed to the sender. The number of configured 
     *    benches is shown, followed by each bench name.
     * 4. Each bench is printed in a formatted way to the sender, listing all the available 
     *    bench regions.
     * 
     * This command is typically used by administrators to see all available bench regions 
     * that have been defined.
     * 
     * @param sender The sender of the command, which can be a player, console, or other entity.
     * @return A boolean indicating whether the command was successfully handled. Always returns `true` 
     *         since this command has a clear success or failure message.
     */
    private boolean handleListBenches(CommandSender sender) {
        if (!sender.hasPermission("benches.list")) {
            sender.sendMessage("§cNo permission!");
            return true;
        }

        List<String> benches = regionManager.getRegionNames();
        sender.sendMessage("§6Configured Benches (§e" + benches.size() + "§6):");
        benches.forEach(name -> sender.sendMessage("§7- §f" + name));
        return true;
    }

    private boolean handleBenchInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("benches.info")) {
            sender.sendMessage("§cNo permission!");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("§cUsage: /benchinfo <name>");
            return false;
        }

        CuboidRegion region = regionManager.getRegion(args[0]);
        if (region == null) {
            sender.sendMessage("§cNo bench found with that name!");
            return true;
        }

        sender.sendMessage("§6Bench Info: §e" + args[0]);
        sender.sendMessage("§7World: §f" + region.getWorldName());
        sender.sendMessage("§7Bounds: §f" + formatLocation(region.getMin()) + " - " + formatLocation(region.getMax()));
        return true;
    }

    private boolean handleRegionCheck(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this!");
            return true;
        }

        Player player = (Player) sender;
        String regionName = regionManager.getRegionAt(player.getLocation());

        if (regionName != null) {
            player.sendMessage("§aYou're in bench region: §e" + regionName);
        } else {
            player.sendMessage("§cNot in any bench region");
        }
        return true;
    }

    private String formatLocation(Location loc) {
        return String.format("X:%.0f Y:%.0f Z:%.0f", loc.getX(), loc.getY(), loc.getZ());
    }
}