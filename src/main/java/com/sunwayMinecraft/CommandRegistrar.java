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

    // Container finder
    ContainerFinderCommands containerCmds =
            new ContainerFinderCommands(init.getContainerFinderManager());
    registerCommand("findcontainers", containerCmds);

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

    // Residency / Storefronts
    if (init.getResidencyManager() != null) {
      ResidencyCommands residencyCmds =
              new ResidencyCommands(init.getResidencyManager());
      registerCommand("residency", residencyCmds);

      StorefrontCommands storefrontCmds =
              new StorefrontCommands(init.getResidencyManager());
      registerCommand("storefront", storefrontCmds);

      ResidencyAdminCommands residencyAdminCmds =
              new ResidencyAdminCommands(
                      init.getResidencyManager(),
                      init.getResidencySelectionManager());
      registerCommand("resadmin", residencyAdminCmds);
    }

    // Districts / Zoning
    if (init.getDistrictManager() != null) {
      DistrictCommands districtCmds =
              new DistrictCommands(init.getDistrictManager());
      registerCommand("district", districtCmds);

      DistrictAdminCommands districtAdminCmds =
              new DistrictAdminCommands(init.getDistrictManager());
      registerCommand("districtadmin", districtAdminCmds);
    }

    // World travel
    WorldTravelCommands worldTravelCmds =
            new WorldTravelCommands(init.getWorldTravelManager());
    registerCommand("mineworld", worldTravelCmds);
    registerCommand("lifeworld", worldTravelCmds);
    registerCommand("mininginfo", worldTravelCmds);

    // Mining world admin controls
    MiningWorldAdminCommands miningAdminCmds =
            new MiningWorldAdminCommands(
                    init.getWorldTravelManager(),
                    init.getMiningWorldEvacuationManager());
    registerCommand("miningopen", miningAdminCmds);
    registerCommand("miningresetpending", miningAdminCmds);
    registerCommand("mininglock", miningAdminCmds);
    registerCommand("miningevacuate", miningAdminCmds);
    registerCommand("miningevaccancel", miningAdminCmds);
    registerCommand("miningstate", miningAdminCmds);

    // City Contracts
    ContractsCommands contractsCmds = new ContractsCommands(init.getContractsManager(), init.getContractVerificationService());
    contractsCmds.setEventModifierService(init.getEventModifierService());
    registerCommand("contracts", contractsCmds);
    plugin.getCommand("contracts").setTabCompleter(contractsCmds);

    ContractAdminCommands contractAdminCmds = new ContractAdminCommands(init.getContractsManager());
    registerCommand("contractadmin", contractAdminCmds);
    plugin.getCommand("contractadmin").setTabCompleter(contractAdminCmds);

    // City Events
    EventsCommands eventsCmds = new EventsCommands(init.getCityEventsManager());
    registerCommand("events", eventsCmds);
    plugin.getCommand("events").setTabCompleter(eventsCmds);

    EventAdminCommands eventAdminCmds = new EventAdminCommands(init.getCityEventsManager());
    registerCommand("eventadmin", eventAdminCmds);
    plugin.getCommand("eventadmin").setTabCompleter(eventAdminCmds);

    // City Overview
    CityCommands cityCmds = new CityCommands(init.getCityOverviewService());
    registerCommand("city", cityCmds);
    plugin.getCommand("city").setTabCompleter(cityCmds);
  }

  private void registerCommand(String name, CommandExecutor executor) {
    if (plugin.getCommand(name) != null) {
      plugin.getCommand(name).setExecutor(executor);
    } else {
      plugin.getLogger().warning("Command '" + name + "' not found in plugin.yml!");
    }
  }
}
