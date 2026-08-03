package com.sunwayMinecraft.contracts.domain;

import org.bukkit.Material;
import java.util.Map;

public record ContractDefinition(
    String id,
    ContractCategory category,
    String name,
    String description,
    double rewardMoney,
    long durationMinutes,
    long cooldownMinutes,
    String startEndpointId,
    String endEndpointId,
    Map<Material, Integer> requiredMaterials,
    String objectiveDescription,
    ContractObjectiveType objectiveType
) {
    /**
     * Compatibility constructor for existing Java callers and configurations. New definitions
     * should explicitly select an objective type where the category default is not suitable.
     */
    public ContractDefinition(String id, ContractCategory category, String name, String description,
                              double rewardMoney, long durationMinutes, long cooldownMinutes,
                              String startEndpointId, String endEndpointId,
                              Map<Material, Integer> requiredMaterials, String objectiveDescription) {
        this(id, category, name, description, rewardMoney, durationMinutes, cooldownMinutes,
                startEndpointId, endEndpointId, requiredMaterials, objectiveDescription,
                defaultObjectiveType(category));
    }

    public static ContractObjectiveType defaultObjectiveType(ContractCategory category) {
        return switch (category) {
            case HAULING, RECOVERY -> ContractObjectiveType.DELIVER_MATERIALS;
            case MAINTENANCE -> ContractObjectiveType.INTERACT_AT_DESTINATION;
            case COURIER, SURVEY -> ContractObjectiveType.REACH_DESTINATION;
        };
    }
}
