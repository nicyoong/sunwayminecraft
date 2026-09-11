package com.sunwayMinecraft.districts;

import com.sunwayMinecraft.districts.config.DistrictsConfigManager;
import com.sunwayMinecraft.districts.domain.DistrictDefinition;
import com.sunwayMinecraft.districts.region.DistrictLocationResolver;
import com.sunwayMinecraft.districts.region.DistrictValidationService;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class DistrictManager {
    private final JavaPlugin plugin;
    private final DistrictsConfigManager configManager;
    private final DistrictLocationResolver resolver;
    private final DistrictValidationService validationService;

    public DistrictManager(JavaPlugin plugin, DistrictsConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.resolver = new DistrictLocationResolver(configManager);
        this.validationService = new DistrictValidationService(plugin, configManager);
    }

    public void initialize() {
        reload();
    }

    public void reload() {
        configManager.reload();
        List<String> errors = validationService.validateAll();
        if (errors.isEmpty()) {
            plugin.getLogger().info("[Districts] Loaded " + configManager.getDistricts().size() + " district(s).");
        } else {
            for (String error : errors) {
                plugin.getLogger().severe("[Districts] " + error);
            }
        }
    }

    public DistrictsConfigManager getConfigManager() {
        return configManager;
    }

    public DistrictDefinition getDistrict(String id) {
        return configManager.getDistrict(id);
    }

    public List<DistrictDefinition> getPublicDistricts() {
        return configManager.getPublicDistricts();
    }

    public List<DistrictDefinition> getAllDistricts() {
        return new ArrayList<>(configManager.getDistricts());
    }

    public DistrictDefinition getDistrictAt(Location location) {
        return resolver.getDistrictAt(location);
    }

    public List<String> validate() {
        return validationService.validateAll();
    }

    public List<String> validateDistricts() {
        return validate();
    }
}
