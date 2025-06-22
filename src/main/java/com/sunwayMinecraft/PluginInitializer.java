package com.sunwayMinecraft;

import com.sunwayMinecraft.beacon.BeaconManager;
import com.sunwayMinecraft.benches.BenchesConfigManager;
import com.sunwayMinecraft.benches.BenchRegionManager;
import com.sunwayMinecraft.cathealer.HealingSystem;
import com.sunwayMinecraft.petfinder.PetFinderManager;
import com.sunwayMinecraft.realtime.RealTimeManager;
import com.sunwayMinecraft.coinflip.*;
import com.sunwayMinecraft.switches.*;
import com.sunwayMinecraft.utils.ConfigLoader;
import net.milkbowl.vault.economy.Economy;

public class PluginInitializer {

  private final SunwayMinecraft plugin;

  // Beacon
  private BeaconManager beaconManager;

  // Benches
  private BenchesConfigManager benchesConfigManager;
  private BenchRegionManager regionManager;

  // Switches
  private LightConfigManager lightConfigManager;
  private SwitchConfigManager switchConfigManager;

  // Cat healing
  // (no fields needed)

  // Pet finder
  private PetFinderManager petFinderManager;

  // Real time
  private RealTimeManager realTimeManager;

  // Coin flip
  private CoinFlipSystem coinFlipSystem;
  private ItemCoinFlipSystem itemCoinFlipSystem;
  private CoinFlipDatabase coinFlipDatabase;

  public PluginInitializer(SunwayMinecraft plugin) {
    this.plugin = plugin;

    // 1) Load main config
    ConfigLoader.getConfig(plugin);

    // 2) Initialize each system
    initBeaconSystem();
    initBenchSystem();
    initSwitchSystem();
    initCatHealingSystem();
    initPetFinderSystem();
    initRealTimeSystem();
    initCoinFlipSystem();
  }

  private void initBeaconSystem() {
    beaconManager = new BeaconManager(plugin);
    beaconManager.initialize();
  }

  private void initBenchSystem() {
    benchesConfigManager = new BenchesConfigManager(plugin);
    regionManager = new BenchRegionManager(plugin, benchesConfigManager);
    new com.sunwayMinecraft.benches.BenchInteractListener(plugin, regionManager).register();
  }

  private void initSwitchSystem() {
    LightConfigManager lightCfgManager = new LightConfigManager(plugin);
    SwitchConfigManager switchCfgManager = new SwitchConfigManager(plugin);
    lightConfigManager.reload();
    switchConfigManager.reload();

    SwitchManager switchManager = new SwitchManager(switchCfgManager, lightCfgManager);
    SwitchListener listener = new SwitchListener(switchManager, switchCfgManager);
    plugin.getServer().getPluginManager().registerEvents(listener, plugin);

    // every tick to check for midnight
    CelestialLightScheduler celestialScheduler =
        new CelestialLightScheduler(switchCfgManager, "world");
    celestialScheduler.runTaskTimer(plugin, 0L, 20L);
  }

  private void initCatHealingSystem() {
    new HealingSystem(plugin).start();
  }

  private void initPetFinderSystem() {
    petFinderManager = new PetFinderManager(plugin);
  }

  private void initRealTimeSystem() {
    realTimeManager = new RealTimeManager();
  }

  private void initCoinFlipSystem() {
    coinFlipDatabase = new CoinFlipDatabase(plugin);
    Economy econ = getEconomy();
    if (econ == null) {
      plugin.getLogger().severe("Coin flip disabled - Vault economy not found!");
      return;
    }
    coinFlipSystem = new CoinFlipSystem(econ, coinFlipDatabase);
    itemCoinFlipSystem = new ItemCoinFlipSystem(coinFlipSystem, coinFlipDatabase);
  }

  private Economy getEconomy() {
    if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) return null;
    return plugin.getServer().getServicesManager().getRegistration(Economy.class).getProvider();
  }

  // ──────── getters for CommandRegistrar ─────────
  public BeaconManager getBeaconManager() {
    return beaconManager;
  }

  public BenchesConfigManager getBenchesConfigManager() {
    return benchesConfigManager;
  }

  public BenchRegionManager getRegionManager() {
    return regionManager;
  }

  public LightConfigManager getLightConfigManager() {
    return lightConfigManager;
  }

  public SwitchConfigManager getSwitchConfigManager() {
    return switchConfigManager;
  }

  public PetFinderManager getPetFinderManager() {
    return petFinderManager;
  }

  public RealTimeManager getRealTimeManager() {
    return realTimeManager;
  }

  public CoinFlipSystem getCoinFlipSystem() {
    return coinFlipSystem;
  }

  public ItemCoinFlipSystem getItemCoinFlipSystem() {
    return itemCoinFlipSystem;
  }

  public CoinFlipDatabase getCoinFlipDatabase() {
    return coinFlipDatabase;
  }
}
