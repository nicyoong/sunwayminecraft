package com.sunwayMinecraft.districts.region;

import com.sunwayMinecraft.districts.config.DistrictsConfigManager;
import com.sunwayMinecraft.districts.domain.DistrictDefinition;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class DistrictValidationService {
    private static final long MAX_REGION_VOLUME = 1_000_000L;

    private final JavaPlugin plugin;
    private final DistrictsConfigManager configManager;

    public DistrictValidationService(JavaPlugin plugin, DistrictsConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

