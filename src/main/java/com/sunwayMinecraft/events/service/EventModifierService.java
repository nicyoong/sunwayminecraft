package com.sunwayMinecraft.events.service;

import com.sunwayMinecraft.contracts.domain.ContractCategory;
import com.sunwayMinecraft.events.domain.ActiveCityEvent;
import com.sunwayMinecraft.events.domain.CityEventDefinition;

import java.util.Optional;

public class EventModifierService {
    private final CityEventsManager eventsManager;

    public EventModifierService(CityEventsManager eventsManager) {
        this.eventsManager = eventsManager;
    }

    public double getRewardMultiplier(ContractCategory category) {
        double maxMultiplier = 1.0;
        
        for (ActiveCityEvent ac : eventsManager.getActiveEvents()) {
            CityEventDefinition def = eventsManager.getConfigManager().getEvent(ac.getEventId());
            if (def != null && def.boostedCategories().contains(category)) {
                maxMultiplier = Math.max(maxMultiplier, def.rewardMultiplier());
            }
        }
        
        return maxMultiplier;
    }

    public Optional<CityEventDefinition> getPrimaryEventForCategory(ContractCategory category) {
        CityEventDefinition bestDef = null;
        double maxMultiplier = 1.0;

        for (ActiveCityEvent ac : eventsManager.getActiveEvents()) {
            CityEventDefinition def = eventsManager.getConfigManager().getEvent(ac.getEventId());
            if (def != null && def.boostedCategories().contains(category)) {
                if (def.rewardMultiplier() > maxMultiplier) {
                    maxMultiplier = def.rewardMultiplier();
                    bestDef = def;
                }
            }
        }
        
        return Optional.ofNullable(bestDef);
    }
}
