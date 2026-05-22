package com.sunwayMinecraft.city;

import com.sunwayMinecraft.PluginInitializer;
import com.sunwayMinecraft.contracts.domain.ContractDefinition;
import com.sunwayMinecraft.districts.domain.DistrictDefinition;
import com.sunwayMinecraft.events.domain.ActiveCityEvent;
import com.sunwayMinecraft.events.domain.CityEventDefinition;
import com.sunwayMinecraft.residency.domain.UnitTenancyRecord;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

public class CityOverviewService {
    private final PluginInitializer init;

    public CityOverviewService(PluginInitializer init) {
        this.init = init;
    }

    public String getCurrentEventName() {
        if (init.getCityEventsManager() == null) return "unavailable";
        List<ActiveCityEvent> active = init.getCityEventsManager().getActiveEvents();
        if (active.isEmpty()) return "None";
        
        ActiveCityEvent first = active.get(0);
        CityEventDefinition def = init.getCityEventsManager().getConfigManager().getEvent(first.getEventId());
        return def != null ? def.name() : "Unknown Event";
    }

    public int getAvailableContractsCount() {
        if (init.getContractsManager() == null) return 0;
        return init.getContractsManager().getContractConfig().getContracts().size();
    }

    public int getBoostedContractsCount() {
        if (init.getContractsManager() == null || init.getEventModifierService() == null) return 0;
        int count = 0;
        for (ContractDefinition def : init.getContractsManager().getContractConfig().getContracts().values()) {
            if (init.getEventModifierService().getPrimaryEventForCategory(def.category()).isPresent()) {
                count++;
            }
        }
        return count;
    }

    public String getDistrictName(Player player) {
        if (init.getDistrictManager() == null) return "unavailable";
        Optional<DistrictDefinition> dist = Optional.ofNullable(init.getDistrictManager().getDistrictAt(player.getLocation()));
        return dist.map(DistrictDefinition::name).orElse("None");
    }

    public String getResidencySummary(Player player) {
        if (init.getResidencyManager() == null) return "unavailable";
        long count = init.getResidencyManager().getRepository().getAllTenancies().stream()
                .filter(t -> player.getUniqueId().equals(t.getTenantPlayerId()))
                .count();
        if (count == 0) return "No active rental";
        return count + " active rental(s)";
    }
}
