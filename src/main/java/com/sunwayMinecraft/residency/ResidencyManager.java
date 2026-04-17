package com.sunwayMinecraft.residency;

import com.sunwayMinecraft.residency.access.PremisesAccessService;
import com.sunwayMinecraft.residency.config.*;
import com.sunwayMinecraft.residency.domain.*;
import com.sunwayMinecraft.residency.leasing.BillingService;
import com.sunwayMinecraft.residency.leasing.RepossessionService;
import com.sunwayMinecraft.residency.listing.DirectoryService;
import com.sunwayMinecraft.residency.region.*;
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

    public void initialize() {
        districtsConfigManager.reload();
        buildingsConfigManager.reload();
        unitsConfigManager.reload();
        pricingConfigManager.reload();
        policyConfigManager.reload();
        repository.load();
        units.clear();
        for (UnitDefinition unit : unitsConfigManager.getUnits()) {
            units.put(unit.getId().toLowerCase(), unit);
            UnitTenancyRecord tenancy = repository.getTenancy(unit.getId());
            if (tenancy.getLeaseState() == LeaseState.VACANT && unit.getListingSettings().isVisible()) {
                tenancy.setLeaseState(LeaseState.LISTED);
                repository.saveTenancy(tenancy);
            }
        }
        List<String> errors = validationService.validateAll();
        for (String error : errors) plugin.getLogger().severe("[Residency] " + error);
    }

    public JavaPlugin getPlugin() { return plugin; }
    public ResidencyRepository getRepository() { return repository; }
    public PremisesAccessService getAccessService() { return accessService; }
    public DirectoryService getDirectoryService() { return directoryService; }
    public BillingService getBillingService() { return billingService; }
    public RepossessionService getRepossessionService() { return repossessionService; }
    public Map<String, UnitDefinition> getUnits() { return units; }
    public UnitDefinition getUnit(String id) { return units.get(id.toLowerCase()); }

    public UnitDefinition getUnitAt(Location location) {
        for (UnitDefinition unit : units.values()) {
            if (unit.getPrimaryRegion().contains(location)) return unit;
            for (Region3i region : unit.getLinkedRegions().values()) if (region.contains(location)) return unit;
        }
        return null;
    }

    public boolean isManagedLocation(Location location) {
        if (location == null) return false;
        for (DistrictDefinition district : districtsConfigManager.getDistricts()) {
            if (district.isEnabled() && district.getRegion().contains(location)) return true;
        }
        return false;
    }

    public PricingProfile getPricingProfile(UnitDefinition unit) {
        PricingProfile profile = pricingConfigManager.getProfile(unit.getPricingProfileId());
        if (profile == null) {
            DistrictDefinition district = districtsConfigManager.getDistrict(unit.getDistrictId());
            if (district != null) profile = pricingConfigManager.getProfile(district.getPricingProfileId());
        }
        return profile;
    }

    public PolicyProfile getPolicyProfile(UnitDefinition unit) {
        PolicyProfile profile = policyConfigManager.getProfile(unit.getPolicyProfileId());
        if (profile == null) {
            DistrictDefinition district = districtsConfigManager.getDistrict(unit.getDistrictId());
            if (district != null) profile = policyConfigManager.getProfile(district.getPolicyProfileId());
        }
        return profile;
    }
}
