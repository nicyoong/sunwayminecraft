package com.sunwayMinecraft.city;

import com.sunwayMinecraft.PluginInitializer;
import com.sunwayMinecraft.districts.DistrictManager;
import com.sunwayMinecraft.residency.ResidencyManager;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CityValidationService {
    private final PluginInitializer init;

    public CityValidationService(PluginInitializer init) {
        this.init = init;
    }

    public Map<String, List<String>> runAll() {
        Map<String, List<String>> results = new LinkedHashMap<>();

        // Districts
        if (init.getDistrictManager() != null) {
            results.put("Districts", init.getDistrictManager().validate());
        } else {
            results.put("Districts", List.of("Subsystem unavailable"));
        }

        // Residency
        if (init.getResidencyManager() != null) {
            results.put("Residency", init.getResidencyManager().validate());
        } else {
            results.put("Residency", List.of("Subsystem unavailable"));
        }

        // Contracts
        if (init.getContractsManager() != null) {
            results.put("Contracts", Collections.emptyList()); // Placeholder for OK
        } else {
            results.put("Contracts", List.of("Subsystem unavailable"));
        }

        // Events
        if (init.getCityEventsManager() != null) {
            results.put("Events", Collections.emptyList()); // Placeholder for OK
        } else {
            results.put("Events", List.of("Subsystem unavailable"));
        }

        return results;
    }
}
