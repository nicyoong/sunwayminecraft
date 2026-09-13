package com.sunwayMinecraft.contracts.service;

import com.sunwayMinecraft.contracts.domain.ContractCategory;
import com.sunwayMinecraft.contracts.domain.ContractDefinition;
import com.sunwayMinecraft.contracts.persistence.ContractDatabase;

import java.util.Map;
import java.util.function.Function;

/**
 * Supply standing earned through contract work, tracked per alignment and
 * rolled up to grand alliances. Hooks a future leaderboard or treasury can
 * read; deliberately lightweight and free of any war logic.
 */
public class ContractSupplyService {
    private final ContractDatabase database;
    private final Function<String, String> allianceResolver;

    public ContractSupplyService(ContractDatabase database, Function<String, String> allianceResolver) {
        this.database = database;
        this.allianceResolver = allianceResolver != null ? allianceResolver : id -> null;
    }

    /**
     * Awards supply points for a completion: every contract is worth one, with
     * extra for the logistics-heavy hauling, emergency and diplomatic kinds.
     */
    public int recordCompletion(ContractDefinition def, String alignmentId) {
        if (def == null || alignmentId == null) return 0;
        int points = 1;
        if (def.category() == ContractCategory.HAULING) points += 2;
        else if (def.category() == ContractCategory.EMERGENCY) points += 2;
        else if (def.category() == ContractCategory.DIPLOMATIC) points += 2;
        database.addSupplyPoints(alignmentId, points);
        return points;
    }

    public int getSupplyPoints(String alignmentId) {
        return alignmentId == null ? 0 : database.getSupplyPoints(alignmentId);
    }

    /** Sums every alignment total whose alignment resolves to the given alliance. */
    public int getSupplyPointsForAlliance(String grandAllianceId) {
        if (grandAllianceId == null) return 0;
        int total = 0;
        for (Map.Entry<String, Integer> entry : database.getAllSupplyTotals().entrySet()) {
            String alliance = allianceResolver.apply(entry.getKey());
            if (grandAllianceId.equalsIgnoreCase(alliance)) {
                total += entry.getValue();
            }
        }
        return total;
    }

    /** Admin/testing adjustment. */
    public void adminAdjustSupply(String alignmentId, int delta) {
        if (alignmentId != null && delta != 0) database.addSupplyPoints(alignmentId, delta);
    }
}
