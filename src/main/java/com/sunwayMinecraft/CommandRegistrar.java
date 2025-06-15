package com.sunwayMinecraft;

import com.sunwayMinecraft.commands.*;
import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;

public class CommandRegistrar {

  private final JavaPlugin plugin;

  public CommandRegistrar(JavaPlugin plugin) {
    this.plugin = plugin;
  }

  public void registerAll(PluginInitializer init) {
    // Beacon commands
    BeaconCommands beaconCmds = new BeaconCommands(init.getBeaconManager());
    registerCommand("pausebeacons", beaconCmds);
    registerCommand("resumebeacons", beaconCmds);
    registerCommand("reloadsunwayconfig", beaconCmds);
    registerCommand("setbeaconticks", beaconCmds);

    // Bench commands
    BenchesCommands benchesCmds =
        new BenchesCommands(init.getRegionManager());
    registerCommand("listbenches", benchesCmds);
    registerCommand("benchinfo", benchesCmds);
    registerCommand("checkbenchregion", benchesCmds);
    registerCommand("reloadsunwaybenches", benchesCmds);

    // Switch commands
    SwitchesCommands switchCmds =
        new SwitchesCommands(plugin, init.getLightConfigManager(), init.getSwitchConfigManager());
    registerCommand("scanlights", switchCmds);
    registerCommand("exportlights", switchCmds);
    registerCommand("listlightregions", switchCmds);
    registerCommand("checklightregion", switchCmds);
    registerCommand("lightinfo", switchCmds);
    registerCommand("reloadsunwayswitches", switchCmds);

    // Pet finder
    PetFinderCommands petCmds = new PetFinderCommands(init.getPetFinderManager());
    registerCommand("findpets", petCmds);
    registerCommand("findpetsinarea", petCmds);

    // Real-time
    RealTimeCommands rtCmds = new RealTimeCommands(init.getRealTimeManager());
    registerCommand("servertime", rtCmds);
    registerCommand("servertimeutc", rtCmds);

    // Coin flip
    CoinFlipCommands cfCmds =
        new CoinFlipCommands(
            init.getCoinFlipSystem(), init.getItemCoinFlipSystem(), init.getCoinFlipDatabase());
    registerCommand("cf", cfCmds);
  }

  private void registerCommand(String name, CommandExecutor executor) {
    if (plugin.getCommand(name) != null) {
      plugin.getCommand(name).setExecutor(executor);
    } else {
      plugin.getLogger().warning("Command '" + name + "' not found in plugin.yml!");
    }
  }
}
