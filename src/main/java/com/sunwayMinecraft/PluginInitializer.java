package com.sunwayMinecraft;

import com.sunwayMinecraft.beacon.BeaconManager;
import com.sunwayMinecraft.benches.BenchesConfigManager;
import com.sunwayMinecraft.benches.RegionManager;
import com.sunwayMinecraft.cathealer.HealingSystem;
import com.sunwayMinecraft.containerfinder.ContainerFinderManager;
import com.sunwayMinecraft.petfinder.PetFinderManager;
import com.sunwayMinecraft.realtime.RealTimeManager;
import com.sunwayMinecraft.residency.admin.AdminSelectionManager;
import com.sunwayMinecraft.residency.ResidencyBootstrap;
import com.sunwayMinecraft.residency.ResidencyManager;
import com.sunwayMinecraft.districts.DistrictBootstrap;
import com.sunwayMinecraft.districts.DistrictManager;
import com.sunwayMinecraft.coinflip.*;
import com.sunwayMinecraft.switches.*;
import com.sunwayMinecraft.worldtravel.*;
import com.sunwayMinecraft.utils.ConfigLoader;
import net.milkbowl.vault.economy.Economy;
import com.sunwayMinecraft.SunwayMinecraft;

public class PluginInitializer {

  private final SunwayMinecraft plugin;

  // Beacon
  private BeaconManager beaconManager;

  // Benches
  private BenchesConfigManager benchesConfigManager;
  private RegionManager regionManager;

  // Switches
  private LightConfigManager lightConfigManager;
  private SwitchConfigManager switchConfigManager;

  // Cat healing
  // (no fields needed)

  // Container finder
  private ContainerFinderManager containerFinderManager;

  // Pet finder
  private PetFinderManager petFinderManager;

  // Real time
  private RealTimeManager realTimeManager;

  // Residency
  private ResidencyManager residencyManager;
  private AdminSelectionManager residencySelectionManager;

  // Districts
  private DistrictManager districtManager;

  // Coin flip
  private CoinFlipSystem coinFlipSystem;
  private ItemCoinFlipSystem itemCoinFlipSystem;
  private CoinFlipDatabase coinFlipDatabase;

  // World travel
  private WorldTravelManager worldTravelManager;
  private MiningWorldEvacuationManager miningWorldEvacuationManager;

  public PluginInitializer(SunwayMinecraft plugin) {
    this.plugin = plugin;

    // 1) Load main config
    ConfigLoader.getConfig(plugin);

    // 2) Initialize each system
    initBeaconSystem();
    initBenchSystem();
    initSwitchSystem();
    initCatHealingSystem();
    initContainerFinderSystem();
    initPetFinderSystem();
    initRealTimeSystem();
    initResidencySystem();
    initDistrictSystem();
    initCoinFlipSystem();
    initWorldTravelSystem();
  }

  private void initBeaconSystem() {
    beaconManager = new BeaconManager(plugin);
    beaconManager.initialize();
  }

  private void initBenchSystem() {
    benchesConfigManager = new BenchesConfigManager(plugin);
    regionManager = new RegionManager(plugin, benchesConfigManager);
    new com.sunwayMinecraft.benches.BenchInteractListener(plugin, regionManager).register();
  }

  private void initSwitchSystem() {
    lightConfigManager = new LightConfigManager(plugin);
    switchConfigManager = new SwitchConfigManager(plugin);
    lightConfigManager.reload();
    switchConfigManager.reload();

    SwitchManager switchManager = new SwitchManager(switchConfigManager, lightConfigManager);
    SwitchListener listener = new SwitchListener(switchManager, switchConfigManager);
    plugin.getServer().getPluginManager().registerEvents(listener, plugin);

    // every tick to check for midnight
    CelestialLightScheduler celestialScheduler =
        new CelestialLightScheduler(switchConfigManager, "world");
    celestialScheduler.runTaskTimer(plugin, 0L, 20L);
  }

  private void initCatHealingSystem() {
    new HealingSystem(plugin).start();
  }

  private void initContainerFinderSystem() {
    containerFinderManager = new ContainerFinderManager(plugin);
  }

  private void initPetFinderSystem() {
    petFinderManager = new PetFinderManager(plugin);
  }

  private void initRealTimeSystem() {
    realTimeManager = new RealTimeManager();
  }

  private void initResidencySystem() {
    residencySelectionManager = new AdminSelectionManager(plugin);
    Economy econ = getEconomy();
    residencyManager = new ResidencyBootstrap(plugin, econ, residencySelectionManager).initialize();
  }

  private void initDistrictSystem() {
    districtManager = new DistrictBootstrap(plugin).initialize();
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

  private void initWorldTravelSystem() {
    worldTravelManager = new WorldTravelManager(plugin);
    worldTravelManager.loadState();
    miningWorldEvacuationManager = new MiningWorldEvacuationManager(plugin, worldTravelManager);

    plugin.getServer().getPluginManager()
            .registerEvents(new MiningWorldListener(worldTravelManager), plugin);
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

  public RegionManager getRegionManager() {
    return regionManager;
  }

  public LightConfigManager getLightConfigManager() {
    return lightConfigManager;
  }

  public SwitchConfigManager getSwitchConfigManager() {
    return switchConfigManager;
  }

  public ContainerFinderManager getContainerFinderManager() {
    return containerFinderManager;
  }

  public PetFinderManager getPetFinderManager() {
    return petFinderManager;
  }

  public RealTimeManager getRealTimeManager() {
    return realTimeManager;
  }

  public ResidencyManager getResidencyManager() {
    return residencyManager;
  }

  public AdminSelectionManager getResidencySelectionManager() {
    return residencySelectionManager;
  }

  public DistrictManager getDistrictManager() {
    return districtManager;
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

  public WorldTravelManager getWorldTravelManager() {
    return worldTravelManager;
  }

  public MiningWorldEvacuationManager getMiningWorldEvacuationManager() {
    return miningWorldEvacuationManager;
  }
}
