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

    public List<String> validateAll() {
        List<String> errors = new ArrayList<>();
        List<DistrictDefinition> districts = new ArrayList<>(configManager.getDistricts());

        for (DistrictDefinition district : districts) {
            if (district.getDisplayName().isBlank()) {
                errors.add("District '" + district.getId() + "' has an empty display name.");
            }
            if (district.getPublicSummary().isBlank()) {
                errors.add("District '" + district.getId() + "' is missing a public summary.");
            }
            if (Bukkit.getWorld(district.getWorld()) == null) {
                errors.add("District '" + district.getId() + "' refers to missing world '" + district.getWorld() + "'.");
            }
            if (district.getPrestigeTier() < 1 || district.getPrestigeTier() > 5) {
                errors.add("District '" + district.getId() + "' prestige tier must be between 1 and 5.");
            }
            if (district.getRegion().getVolume() > MAX_REGION_VOLUME) {
                errors.add("District '" + district.getId() + "' exceeds max volume of " + MAX_REGION_VOLUME + " blocks.");
            }
        }

        for (int i = 0; i < districts.size(); i++) {
            for (int j = i + 1; j < districts.size(); j++) {
                DistrictDefinition a = districts.get(i);
                DistrictDefinition b = districts.get(j);
                if (!a.isEnabled() || !b.isEnabled()) continue;
                if (a.getRegion().overlapsVolume(b.getRegion())) {
                    errors.add("Districts '" + a.getId() + "' and '" + b.getId() + "' overlap.");
                }
            }
        }

        return errors;
    }
}
