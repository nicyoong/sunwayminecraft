package com.sunwayMinecraft.contracts.domain;

import net.kyori.adventure.text.Component;
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
    String objectiveDescription
) {}
