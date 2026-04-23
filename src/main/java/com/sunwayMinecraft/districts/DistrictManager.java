package com.sunwayMinecraft.districts;

import com.sunwayMinecraft.districts.config.DistrictsConfigManager;
import com.sunwayMinecraft.districts.domain.DistrictDefinition;
import com.sunwayMinecraft.districts.region.DistrictResolver;
import com.sunwayMinecraft.districts.region.DistrictValidationService;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class DistrictManager {
    private final JavaPlugin plugin;
    private final DistrictsConfigManager configManager;
    private final DistrictResolver resolver;
    private final DistrictValidationService validationService;

    public DistrictManager(JavaPlugin plugin, DistrictsConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.resolver = new DistrictResolver(configManager);
        this.validationService = new DistrictValidationService(plugin, configManager);
    }

