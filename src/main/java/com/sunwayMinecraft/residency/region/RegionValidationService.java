package com.sunwayMinecraft.residency.region;

import com.sunwayMinecraft.residency.config.BuildingsConfigManager;
import com.sunwayMinecraft.residency.config.DistrictsConfigManager;
import com.sunwayMinecraft.residency.config.UnitsConfigManager;
import com.sunwayMinecraft.residency.domain.BuildingDefinition;
import com.sunwayMinecraft.residency.domain.DistrictDefinition;
import com.sunwayMinecraft.residency.domain.UnitDefinition;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class RegionValidationService {
    private final JavaPlugin plugin;
    private final DistrictsConfigManager districts;
    private final BuildingsConfigManager buildings;
    private final UnitsConfigManager units;

    public RegionValidationService(JavaPlugin plugin, DistrictsConfigManager districts, BuildingsConfigManager buildings, UnitsConfigManager units) {
        this.plugin = plugin;
        this.districts = districts;
        this.buildings = buildings;
        this.units = units;
    }

    public List<String> validateAll() {
        List<String> errors = new ArrayList<>();
        for (DistrictDefinition district : districts.getDistricts()) {
            if (Bukkit.getWorld(district.getWorld()) == null) errors.add("District " + district.getId() + " uses missing world " + district.getWorld());
        }
        for (BuildingDefinition building : buildings.getBuildings()) {
            DistrictDefinition district = districts.getDistrict(building.getDistrictId());
            if (district == null) {
                errors.add("Building " + building.getId() + " references missing district " + building.getDistrictId());
                continue;
            }
            if (!district.getRegion().contains(building.getPrimaryRegion())) errors.add("Building " + building.getId() + " is outside district " + district.getId());
        }
        List<UnitDefinition> allUnits = new ArrayList<>(units.getUnits());
        for (UnitDefinition unit : allUnits) {
            DistrictDefinition district = districts.getDistrict(unit.getDistrictId());
            if (district == null) {
                errors.add("Unit " + unit.getId() + " references missing district " + unit.getDistrictId());
                continue;
            }
            if (!district.getRegion().contains(unit.getPrimaryRegion())) errors.add("Unit " + unit.getId() + " is outside district " + district.getId());
            if (unit.getBuildingId() != null) {
                BuildingDefinition building = buildings.getBuilding(unit.getBuildingId());
                if (building == null) errors.add("Unit " + unit.getId() + " references missing building " + unit.getBuildingId());
                else if (!building.getPrimaryRegion().contains(unit.getPrimaryRegion())) errors.add("Unit " + unit.getId() + " is outside building " + building.getId());
            }
        }
        for (int i = 0; i < allUnits.size(); i++) {
            for (int j = i + 1; j < allUnits.size(); j++) {
                if (allUnits.get(i).getPrimaryRegion().overlaps(allUnits.get(j).getPrimaryRegion())) {
                    errors.add("Units overlap: " + allUnits.get(i).getId() + " and " + allUnits.get(j).getId());
                }
            }
        }
        if (errors.isEmpty()) plugin.getLogger().info("Residency region validation passed.");
        return errors;
    }
}
