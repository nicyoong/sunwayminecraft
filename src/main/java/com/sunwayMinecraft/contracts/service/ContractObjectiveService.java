package com.sunwayMinecraft.contracts.service;

import com.sunwayMinecraft.contracts.domain.ActiveContract;
import com.sunwayMinecraft.contracts.domain.ContractDefinition;
import com.sunwayMinecraft.contracts.domain.ContractEndpoint;
import com.sunwayMinecraft.contracts.domain.ContractObjectiveType;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/** Records event-driven contract objectives. */
public class ContractObjectiveService {
    private final ContractsManager manager;

    public ContractObjectiveService(ContractsManager manager) {
        this.manager = manager;
    }

    /**
     * Records a right-click at a location. Returns the number of objectives newly completed.
     */
    public int recordInteraction(Player player, Location location) {
        if (location == null || location.getWorld() == null) return 0;

        int completed = 0;
        for (ActiveContract active : manager.getPersistence().getPlayerContracts(player.getUniqueId())) {
            ContractDefinition definition = manager.getContractConfig().getContract(active.getContractId());
            if (!player.getUniqueId().equals(active.getPlayerUuid()) || definition == null || active.isExpired() || active.isObjectiveComplete()
                    || definition.objectiveType() != ContractObjectiveType.INTERACT_AT_DESTINATION) {
                continue;
            }

            ContractEndpoint endpoint = manager.getEndpointConfig().getEndpoint(definition.endEndpointId());
            if (isWithinEndpoint(location, endpoint)) {
                active.completeObjective();
                completed++;
            }
        }

        if (completed > 0) {
            manager.getPersistence().save();
        }
        return completed;
    }

    static boolean isWithinEndpoint(Location location, ContractEndpoint endpoint) {
        if (location == null || location.getWorld() == null || endpoint == null || endpoint.location() == null || endpoint.location().getWorld() == null
                || !location.getWorld().equals(endpoint.location().getWorld())) {
            return false;
        }
        return location.distanceSquared(endpoint.location()) <= endpoint.radius() * endpoint.radius();
    }
}
