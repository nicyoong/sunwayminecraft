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
import com.sunwayMinecraft.districts.service.DistrictAlignmentService;
import com.sunwayMinecraft.districts.DistrictManager;
import com.sunwayMinecraft.coinflip.*;
import com.sunwayMinecraft.switches.*;
import com.sunwayMinecraft.worldtravel.*;
import com.sunwayMinecraft.alignments.config.AlignmentConfigManager;
import com.sunwayMinecraft.alignments.config.AlignmentPerksConfig;
import com.sunwayMinecraft.alignments.config.AlignmentProgressionConfig;
import com.sunwayMinecraft.alignments.config.AlignmentSettingsConfig;
import com.sunwayMinecraft.alignments.listener.AlignmentChatListener;
import com.sunwayMinecraft.alignments.listener.AlignmentPlayerListener;
import com.sunwayMinecraft.alignments.persistence.AlignmentRepository;
import com.sunwayMinecraft.alignments.persistence.AlignmentSeasonRepository;
import com.sunwayMinecraft.alignments.service.AlignmentChatService;
import com.sunwayMinecraft.alignments.service.AlignmentCooldownManager;
import com.sunwayMinecraft.alignments.service.AlignmentMembershipCache;
import com.sunwayMinecraft.alignments.service.AlignmentPerkService;
import com.sunwayMinecraft.alignments.service.AlignmentRankService;
import com.sunwayMinecraft.alignments.service.AlignmentScoreService;
import com.sunwayMinecraft.alignments.service.AlignmentSeasonService;
import com.sunwayMinecraft.alignments.service.AlignmentService;
import com.sunwayMinecraft.city.CityOverviewService;
import com.sunwayMinecraft.districts.DistrictManager;
import com.sunwayMinecraft.city.CityValidationService;
import com.sunwayMinecraft.city.metrics.CityMetricsManager;
import com.sunwayMinecraft.contracts.config.*;
import com.sunwayMinecraft.contracts.persistence.ContractPersistenceService;
import com.sunwayMinecraft.contracts.persistence.ContractDatabase;
import com.sunwayMinecraft.contracts.service.*;
import com.sunwayMinecraft.contracts.listener.ContractObjectiveListener;
import com.sunwayMinecraft.events.config.*;
import com.sunwayMinecraft.events.persistence.EventPersistenceService;
import com.sunwayMinecraft.events.service.*;
import com.sunwayMinecraft.utils.ConfigLoader;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import com.sunwayMinecraft.SunwayMinecraft;

@SuppressWarnings("this-escape")
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
  private DistrictAlignmentService districtAlignmentService;

  // Coin flip
  private CoinFlipSystem coinFlipSystem;
  private ItemCoinFlipSystem itemCoinFlipSystem;
  private CoinFlipDatabase coinFlipDatabase;

  // World travel
  private WorldTravelManager worldTravelManager;
  private MiningWorldEvacuationManager miningWorldEvacuationManager;

  // City Contracts
  private ContractsManager contractsManager;
  private ContractVerificationService contractVerificationService;
  private ContractPersistenceService contractPersistence;
  private ContractDiplomacySettings contractDiplomacySettings;
  private ContractTemplateConfigManager contractTemplateConfig;
  private ContractDiplomacyService contractDiplomacyService;
  private ContractSupplyService contractSupplyService;
  private ContractSabotageService contractSabotageService;
  private DynamicContractService dynamicContractService;

  // City Events
  private CityEventsManager cityEventsManager;
  private EventModifierService eventModifierService;

  // City Metrics
  private CityMetricsManager cityMetricsManager;

  // City Integration
  private CityOverviewService cityOverviewService;
  private CityValidationService cityValidationService;

  // Triple Alliance alignments
  private AlignmentConfigManager alignmentConfigManager;
  private AlignmentSettingsConfig alignmentSettings;
  private AlignmentProgressionConfig alignmentProgression;
  private AlignmentRepository alignmentRepository;
  private AlignmentRankService alignmentRankService;
  private AlignmentMembershipCache alignmentCache;
  private AlignmentPerksConfig alignmentPerksConfig;
  private AlignmentSeasonRepository alignmentSeasonRepository;
  private AlignmentSeasonService alignmentSeasonService;
  private AlignmentScoreService alignmentScoreService;
  private AlignmentPerkService alignmentPerkService;
  private AlignmentCooldownManager alignmentCooldownManager;
  private AlignmentService alignmentService;
  private AlignmentChatService alignmentChatService;

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
    initMetricsSystem();
    initContractsSystem();
    initEventsSystem();
    initCityIntegration();
    initAlignmentSystem();
    initContractsStrategicSystem();
  }

  private void initCityIntegration() {
    cityOverviewService = new CityOverviewService(this);
    cityValidationService = new CityValidationService(this);
  }

  private void initMetricsSystem() {
    cityMetricsManager = new CityMetricsManager(plugin);
    cityMetricsManager.initialize();

    // persist city metrics every 5 minutes so counters survive restarts
    plugin.getServer().getScheduler().runTaskTimer(plugin, cityMetricsManager::save, 6000L, 6000L);
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
    DistrictBootstrap districtBootstrap = new DistrictBootstrap(plugin);
    districtManager = districtBootstrap.initialize();
    districtAlignmentService = districtBootstrap.getAlignmentService();
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

  private void initContractsSystem() {
    ContractConfigManager contractConfig = new ContractConfigManager(plugin);
    contractConfig.load();
    EndpointConfigManager endpointConfig = new EndpointConfigManager(plugin);
    endpointConfig.load();
    SettingsConfigManager settingsConfig = new SettingsConfigManager(plugin);
    settingsConfig.load();
    contractPersistence = new ContractPersistenceService(plugin);

    Economy econ = getEconomy();
    contractsManager = new ContractsManager(plugin, contractConfig, endpointConfig, settingsConfig, contractPersistence, econ);
    contractsManager.setAlignmentLookup(uuid -> alignmentService != null
            ? alignmentService.getAlignmentId(uuid) : java.util.Optional.empty());
    contractsManager.validateEndpointReferences();
    contractVerificationService = new ContractVerificationService(contractsManager);
    plugin.getServer().getPluginManager().registerEvents(
        new ContractObjectiveListener(new ContractObjectiveService(contractsManager)), plugin);
    plugin.getServer().getScheduler().runTaskTimer(plugin, contractsManager::cleanupExpiredContracts, 20L, 1200L);
  }

  private void initEventsSystem() {
    EventConfigManager eventConfig = new EventConfigManager(plugin);
    eventConfig.load();
    EventSettingsManager eventSettings = new EventSettingsManager(plugin);
    eventSettings.load();
    EventPersistenceService persistence = new EventPersistenceService(plugin);
    
    cityEventsManager = new CityEventsManager(plugin, eventConfig, eventSettings, persistence);
    cityEventsManager.initialize();
    
    cityEventsManager.setMetricsManager(cityMetricsManager);
    
    eventModifierService = new EventModifierService(cityEventsManager);
    
    // Inject into contracts
    if (contractsManager != null) {
        contractsManager.setEventModifierService(eventModifierService);
        contractsManager.setMetricsManager(cityMetricsManager);
    }
  }

  private void initAlignmentSystem() {
    alignmentSettings = new AlignmentSettingsConfig(plugin);
    alignmentSettings.load();
    alignmentConfigManager = new AlignmentConfigManager(plugin);
    alignmentConfigManager.load();
    alignmentRepository = new AlignmentRepository(plugin);
    if (!alignmentRepository.isAvailable()) {
      plugin.getLogger()
          .warning("Alignment membership storage is unavailable; alignment changes are disabled");
    }
    alignmentProgression = new AlignmentProgressionConfig(plugin);
    alignmentProgression.load();
    alignmentRankService = new AlignmentRankService(alignmentProgression);
    alignmentCache =
        new AlignmentMembershipCache(alignmentConfigManager, alignmentRepository, alignmentRankService);
    alignmentCooldownManager = new AlignmentCooldownManager(alignmentRepository);
    alignmentPerksConfig = new AlignmentPerksConfig(plugin);
    alignmentPerksConfig.load();
    alignmentPerkService =
        new AlignmentPerkService(
            plugin, alignmentPerksConfig, alignmentConfigManager, alignmentRankService,
            alignmentCache, buildDistrictGate());
    alignmentService =
        new AlignmentService(
            alignmentConfigManager, alignmentSettings, alignmentRepository,
            alignmentCooldownManager, alignmentCache, alignmentPerkService);
    if (contractsManager != null) {
      contractsManager.setReputationRewarder((uuid, delta) -> {
        if (delta != 0) {
          alignmentService.adjustReputation(uuid, delta);
        }
      });
    }
    alignmentChatService =
        new AlignmentChatService(alignmentSettings, alignmentConfigManager, alignmentCache);
    alignmentSeasonRepository = new AlignmentSeasonRepository(plugin);
    alignmentSeasonService =
        new AlignmentSeasonService(
            plugin, alignmentProgression, alignmentSeasonRepository, alignmentRepository,
            alignmentConfigManager, alignmentCache);
    alignmentSeasonService.start();
    alignmentScoreService =
        new AlignmentScoreService(alignmentProgression, alignmentConfigManager, alignmentRepository);
    alignmentPerkService.start();

    new AlignmentPlayerListener(alignmentCache).register(plugin);
    new AlignmentChatListener(
            alignmentSettings, alignmentConfigManager, alignmentCache,
            alignmentRankService, alignmentProgression)
        .register(plugin);
    alignmentCache.loadOnlinePlayers();

    // hourly season check (aligned with the metrics save cadence)
    plugin.getServer().getScheduler().runTaskTimer(
        plugin, alignmentSeasonService::checkSeason, 6000L, 6000L);
  }

  /**
   * Builds the contract strategic layer after the alignment system, so the
   * alignment -> grand-alliance and reputation hooks resolve against live
   * services. No-op if the contracts system failed to initialise.
   */
  private void initContractsStrategicSystem() {
    if (contractsManager == null) return;
    contractDiplomacySettings = new ContractDiplomacySettings(plugin);
    contractDiplomacySettings.load();
    contractTemplateConfig = new ContractTemplateConfigManager(plugin);
    contractTemplateConfig.load();
    ContractDatabase contractsDb = contractsManager.getPersistence().getDatabase();

    java.util.function.Function<String, String> allianceResolver = alignmentId ->
        (alignmentConfigManager == null || alignmentId == null) ? null
            : alignmentConfigManager.getAlignment(alignmentId)
                .map(def -> def.grandAlliance().getId()).orElse(null);

    contractDiplomacyService =
        new ContractDiplomacyService(contractsDb, contractDiplomacySettings, allianceResolver);
    contractSupplyService = new ContractSupplyService(contractsDb, allianceResolver);
    contractSabotageService = new ContractSabotageService(
        contractsDb, contractsManager.getPersistence(), contractsManager.getContractConfig(),
        contractDiplomacySettings, contractDiplomacyService, getEconomy(),
        uuid -> alignmentService != null ? alignmentService.getAlignmentId(uuid)
            : java.util.Optional.empty(),
        (uuid, delta) -> {
          if (alignmentService != null && delta != 0) alignmentService.adjustReputation(uuid, delta);
        },
        plugin);
    dynamicContractService = new DynamicContractService(
        contractsManager.getContractConfig(), contractTemplateConfig,
        contractsManager.getEndpointConfig(), contractsDb, contractDiplomacySettings, plugin);
    dynamicContractService.loadPersisted();

    contractsManager.setDiplomacyService(contractDiplomacyService);
    contractsManager.setSupplyService(contractSupplyService);

    // periodically retire expired dynamic contracts (aligned with cleanup cadence)
    plugin.getServer().getScheduler().runTaskTimer(
        plugin, dynamicContractService::expireOverdue, 600L, 1200L);
    // optional scheduled emergency generation
    long intervalMinutes = contractDiplomacySettings.getEmergencyIntervalMinutes();
    String template = contractDiplomacySettings.getEmergencyTemplate();
    if (intervalMinutes > 0 && template != null && !template.isBlank()) {
      long ticks = intervalMinutes * 60L * 20L;
      plugin.getServer().getScheduler().runTaskTimer(plugin,
          () -> dynamicContractService.generate(template), ticks, ticks);
    }
  }

  public ContractDiplomacySettings getContractDiplomacySettings() { return contractDiplomacySettings; }
  public ContractDiplomacyService getContractDiplomacyService() { return contractDiplomacyService; }
  public ContractSupplyService getContractSupplyService() { return contractSupplyService; }
  public ContractSabotageService getContractSabotageService() { return contractSabotageService; }
  public DynamicContractService getDynamicContractService() { return dynamicContractService; }

  /**
   * District gate for perks: perks apply only inside enabled districts when
   * apply_in_any_district is false. Null when the district system is absent.
   */
  private java.util.function.Predicate<org.bukkit.entity.Player> buildDistrictGate() {
    DistrictManager districts = getDistrictManager();
    if (districts == null) {
      return null;
    }
    return player -> {
      var district = districts.getDistrictAt(player.getLocation());
      return district != null
          && (alignmentPerksConfig.isApplyInDisabledDistricts() || district.isEnabled());
    };
  }

  private Economy getEconomy() {
    if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) return null;
    RegisteredServiceProvider<Economy> registration =
        plugin.getServer().getServicesManager().getRegistration(Economy.class);
    if (registration == null) {
      plugin.getLogger().severe("Vault is present but no economy provider is registered; economy features are disabled");
      return null;
    }
    return registration.getProvider();
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

  public DistrictAlignmentService getDistrictAlignmentService() {
    return districtAlignmentService;
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

  public ContractsManager getContractsManager() {
    return contractsManager;
  }

  public ContractVerificationService getContractVerificationService() {
    return contractVerificationService;
  }

  public ContractPersistenceService getContractPersistence() {
    return contractPersistence;
  }

  public CityEventsManager getCityEventsManager() {
    return cityEventsManager;
  }

  public EventModifierService getEventModifierService() {
    return eventModifierService;
  }

  public CityMetricsManager getCityMetricsManager() {
    return cityMetricsManager;
  }

  public CityOverviewService getCityOverviewService() {
    return cityOverviewService;
  }

  public CityValidationService getCityValidationService() {
    return cityValidationService;
  }

  public AlignmentConfigManager getAlignmentConfigManager() {
    return alignmentConfigManager;
  }

  public AlignmentSettingsConfig getAlignmentSettings() {
    return alignmentSettings;
  }

  public AlignmentProgressionConfig getAlignmentProgression() {
    return alignmentProgression;
  }

  public AlignmentRankService getAlignmentRankService() {
    return alignmentRankService;
  }

  public AlignmentPerksConfig getAlignmentPerksConfig() {
    return alignmentPerksConfig;
  }

  public AlignmentSeasonRepository getAlignmentSeasonRepository() {
    return alignmentSeasonRepository;
  }

  public AlignmentSeasonService getAlignmentSeasonService() {
    return alignmentSeasonService;
  }

  public AlignmentScoreService getAlignmentScoreService() {
    return alignmentScoreService;
  }

  public AlignmentPerkService getAlignmentPerkService() {
    return alignmentPerkService;
  }

  public AlignmentRepository getAlignmentRepository() {
    return alignmentRepository;
  }

  public AlignmentMembershipCache getAlignmentCache() {
    return alignmentCache;
  }

  public AlignmentChatService getAlignmentChatService() {
    return alignmentChatService;
  }

  public AlignmentService getAlignmentService() {
    return alignmentService;
  }
}
