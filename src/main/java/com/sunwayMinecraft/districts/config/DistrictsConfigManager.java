package com.sunwayMinecraft.districts.config;

import com.sunwayMinecraft.districts.domain.ApprovalBias;
import com.sunwayMinecraft.districts.domain.DistrictDefinition;
import com.sunwayMinecraft.districts.domain.DistrictType;
import com.sunwayMinecraft.districts.region.Region3i;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

public class DistrictsConfigManager {
    private static final String FILE_NAME = "districts.yml";
    private final JavaPlugin plugin;
    private final Map<String, DistrictDefinition> districts = new LinkedHashMap<>();
    private YamlConfiguration config;

    public DistrictsConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

