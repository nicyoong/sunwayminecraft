package com.sunwayMinecraft;

import com.sunwayMinecraft.beacon.BeaconManager;
import com.sunwayMinecraft.benches.BenchesConfigManager;
import com.sunwayMinecraft.commands.BeaconCommands;
import com.sunwayMinecraft.benches.RegionManager;
import com.sunwayMinecraft.cathealer.HealingSystem;
import com.sunwayMinecraft.commands.BenchesCommands;
import com.sunwayMinecraft.benches.BenchInteractListener;
import com.sunwayMinecraft.commands.PetFinderCommands;
import com.sunwayMinecraft.commands.SwitchesCommands;
import com.sunwayMinecraft.petfinder.PetFinderManager;
import com.sunwayMinecraft.switches.*;
import com.sunwayMinecraft.realtime.RealTimeManager;
import com.sunwayMinecraft.commands.RealTimeCommands;
import com.sunwayMinecraft.coinflip.CoinFlipSystem;
import com.sunwayMinecraft.coinflip.ItemCoinFlipSystem;
import com.sunwayMinecraft.commands.CoinFlipCommands;
import net.milkbowl.vault.economy.Economy;
import com.sunwayMinecraft.utils.ConfigLoader;
import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Level;

public final class SunwayMinecraft extends JavaPlugin {

  // Existing managers
  private BeaconManager beaconManager;
  private BenchesConfigManager benchesConfigManager;
  private RegionManager regionManager;
  private PetFinderManager petFinderManager;

  // Switch system additions
  private LightConfigManager lightConfigManager;
  private SwitchConfigManager switchConfigManager;
  private SwitchManager switchManager;
  private SwitchListener switchListener;
  private CelestialLightScheduler celestialScheduler;

  // Real Time
  private RealTimeManager realTimeManager;

  private CoinFlipSystem coinFlipSystem;
  private ItemCoinFlipSystem itemCoinFlipSystem;

  @Override
  public void onEnable() {
    getLogger().log(Level.INFO, "Enabling SunwayMinecraft plugin...");

    // Load main configuration
    ConfigLoader.getConfig(this);

    // Initialize systems
    initializeBeaconSystem();
    initializeBenchSystem();
    initializeSwitchSystem();
    initializeCatHealingSystem();
    initializePetFinderSystem();
    initializeRealTimeSystem();
    initializeCoinFlipSystem();

    // Register commands
    registerCommands();

    getLogger().log(Level.INFO, "SunwayMinecraft plugin has been enabled.");
  }

  // Modified command registration
  private void registerCommands() {
    // Beacon commands
    BeaconCommands beaconCommands = new BeaconCommands(beaconManager);
    registerCommand("pausebeacons", beaconCommands);
    registerCommand("resumebeacons", beaconCommands);
    registerCommand("reloadsunwayconfig", beaconCommands);
    registerCommand("setbeaconticks", beaconCommands);

    // Bench commands
    BenchesCommands benchesCommands = new BenchesCommands(benchesConfigManager, regionManager);
    registerCommand("listbenches", benchesCommands);
    registerCommand("benchinfo", benchesCommands);
    registerCommand("checkbenchregion", benchesCommands);
    registerCommand("reloadsunwaybenches", benchesCommands);

    // Switch system commands
    SwitchesCommands switchesCommands =
        new SwitchesCommands(this, lightConfigManager, switchConfigManager);
    registerCommand("scanlights", switchesCommands);
    registerCommand("exportlights", switchesCommands);
    registerCommand("listlightregions", switchesCommands);
    registerCommand("checklightregion", switchesCommands);
    registerCommand("lightinfo", switchesCommands);
    registerCommand("reloadsunwayswitches", switchesCommands);

    // Pet finder commands
    PetFinderCommands petFinderCommands = new PetFinderCommands(petFinderManager);
    registerCommand("findpets", petFinderCommands);
    registerCommand("findpetsinarea", petFinderCommands);

    // Real Time commands
    RealTimeCommands realTimeCommands = new RealTimeCommands(realTimeManager);
    registerCommand("servertime", realTimeCommands);
    registerCommand("servertimeutc", realTimeCommands);

    // Coin Flip commands
    CoinFlipCommands coinFlipCommands = new CoinFlipCommands(coinFlipSystem, itemCoinFlipSystem);
    registerCommand("cf", coinFlipCommands);
  }

  // Rest of the existing class remains unchanged
  @Override
  public void onDisable() {
    getLogger().log(Level.INFO, "Disabling SunwayMinecraft plugin...");
  }

  private void initializeBenchSystem() {
    benchesConfigManager = new BenchesConfigManager(this);
    regionManager = new RegionManager(this, benchesConfigManager);
    BenchInteractListener listener = new BenchInteractListener(this, regionManager);
    listener.register();
  }

  private void registerCommand(String commandName, CommandExecutor executor) {
    if (getCommand(commandName) != null) {
      getCommand(commandName).setExecutor(executor);
    } else {
      getLogger().warning("Command '" + commandName + "' not found in plugin.yml!");
    }
  }

  private void initializeBeaconSystem() {
    beaconManager = new BeaconManager(this);
    beaconManager.initialize();
  }

  private void initializeSwitchSystem() {
    // Initialize managers first
    lightConfigManager = new LightConfigManager(this);
    switchConfigManager = new SwitchConfigManager(this);

    // Load initial configurations
    lightConfigManager.reload();
    switchConfigManager.reload();

    // Create switch manager with initialized configs
    switchManager = new SwitchManager(switchConfigManager, lightConfigManager);

    // Register listener with proper dependencies
    SwitchListener listener = new SwitchListener(switchManager, switchConfigManager);
    getServer().getPluginManager().registerEvents(listener, this);
    startLightSchedulers();
  }

  private void startLightSchedulers() {
    // Check every second (20 ticks) for midnight
    String targetWorld = "world"; // Set your actual world name here
    celestialScheduler = new CelestialLightScheduler(switchConfigManager, targetWorld);
    celestialScheduler.runTaskTimer(this, 0L, 20L);
  }

  private void initializeCatHealingSystem() {
    new HealingSystem(this).start();
  }

  private void initializePetFinderSystem() {
    petFinderManager = new PetFinderManager(this);
  }

  private void initializeRealTimeSystem() {
    realTimeManager = new RealTimeManager();
  }

  private void initializeCoinFlipSystem() {
    Economy econ = getEconomy();
    if (econ == null) {
      getLogger().severe("Coin flip disabled - Vault economy not found!");
      return;
    }

    // Create both systems
    coinFlipSystem = new CoinFlipSystem(econ);
    itemCoinFlipSystem = new ItemCoinFlipSystem(coinFlipSystem);
  }

  // Add this if you don't have an economy getter
  private Economy getEconomy() {
    if (getServer().getPluginManager().getPlugin("Vault") == null) return null;
    return getServer().getServicesManager().getRegistration(Economy.class).getProvider();
  }
}
