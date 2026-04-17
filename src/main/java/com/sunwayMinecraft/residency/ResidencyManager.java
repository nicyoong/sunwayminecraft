package com.sunwayMinecraft.residency;

import com.sunwayMinecraft.residency.access.PremisesAccessService;
import com.sunwayMinecraft.residency.config.*;
import com.sunwayMinecraft.residency.domain.*;
import com.sunwayMinecraft.residency.leasing.BillingService;
import com.sunwayMinecraft.residency.leasing.RepossessionService;
import com.sunwayMinecraft.residency.listing.DirectoryService;
import com.sunwayMinecraft.residency.region.RegionValidationService;
import com.sunwayMinecraft.residency.storage.ResidencyRepository;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ResidencyManager {
    private final JavaPlugin plugin;
    private final DistrictsConfigManager districtsConfigManager;
    private final BuildingsConfigManager buildingsConfigManager;
    private final UnitsConfigManager unitsConfigManager;
    private final PricingConfigManager pricingConfigManager;
    private final PolicyConfigManager policyConfigManager;
    private final ResidencyRepository repository;
    private final BillingService billingService;
    private final PremisesAccessService accessService;
    private final DirectoryService directoryService;
    private final RepossessionService repossessionService;
    private final RegionValidationService validationService;
    private final Map<String, UnitDefinition> units = new LinkedHashMap<>();

    public ResidencyManager(JavaPlugin plugin, DistrictsConfigManager districtsConfigManager, BuildingsConfigManager buildingsConfigManager,
                            UnitsConfigManager unitsConfigManager, PricingConfigManager pricingConfigManager, PolicyConfigManager policyConfigManager,
                            ResidencyRepository repository, BillingService billingService) {
        this.plugin = plugin;
        this.districtsConfigManager = districtsConfigManager;
        this.buildingsConfigManager = buildingsConfigManager;
        this.unitsConfigManager = unitsConfigManager;
        this.pricingConfigManager = pricingConfigManager;
        this.policyConfigManager = policyConfigManager;
        this.repository = repository;
        this.billingService = billingService;
        this.accessService = new PremisesAccessService(this);
        this.directoryService = new DirectoryService(this);
        this.repossessionService = new RepossessionService(this);
        this.validationService = new RegionValidationService(plugin, districtsConfigManager, buildingsConfigManager, unitsConfigManager);
    }

