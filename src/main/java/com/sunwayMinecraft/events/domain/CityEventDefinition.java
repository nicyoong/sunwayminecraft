package com.sunwayMinecraft.events.domain;

import com.sunwayMinecraft.contracts.domain.ContractCategory;
import java.util.Set;

public record CityEventDefinition(
    String id,
    CityEventType type,
    String name,
    String description,
    EventScope scope,
    double rewardMultiplier,
    Set<ContractCategory> boostedCategories,
    long defaultDurationMinutes
) {}
