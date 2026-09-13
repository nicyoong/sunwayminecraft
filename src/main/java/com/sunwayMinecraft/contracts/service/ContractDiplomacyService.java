package com.sunwayMinecraft.contracts.service;

import com.sunwayMinecraft.contracts.config.ContractDiplomacySettings;
import com.sunwayMinecraft.contracts.domain.ContractCampusRoute;
import com.sunwayMinecraft.contracts.domain.ContractCategory;
import com.sunwayMinecraft.contracts.domain.ContractDefinition;
import com.sunwayMinecraft.contracts.domain.InfluenceRecord;
import com.sunwayMinecraft.contracts.persistence.ContractDatabase;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * Turns contract completions into strategic influence. Runs without the
 * district control system: origin/destination alliances are resolved through
 * an injected alignment -> grand-alliance lookup (nullable per side).
 */
public class ContractDiplomacyService {
    private final ContractDatabase database;
    private final ContractDiplomacySettings settings;
    private final Function<String, String> allianceResolver;

    public ContractDiplomacyService(ContractDatabase database, ContractDiplomacySettings settings,
                                    Function<String, String> allianceResolver) {
        this.database = database;
        this.settings = settings;
        this.allianceResolver = allianceResolver != null ? allianceResolver : id -> null;
    }

    /**
     * Records the influence a completion is worth. The credited alignment is the
     * completer's (falling back to the contract origin); a diplomatic contract
     * multiplies, an emergency one adds a flat bonus, and a cross-campus or
     * cross-grand-alliance route adds extra influence.
     */
    public int recordCompletion(ContractDefinition def, UUID completerUuid, String completerAlignment) {
        if (!settings.isEnabled() || def == null) return 0;
        ContractCampusRoute route = def.campusRoute();
        String origin = route.originAlignment() != null ? route.originAlignment() : completerAlignment;
        String destination = route.destinationAlignment();
        String credited = completerAlignment != null ? completerAlignment : origin;

        int influence = settings.getInfluencePerCompletion();
        boolean crossCampus = route.originCampus() != null && route.destinationCampus() != null
                && !route.originCampus().equalsIgnoreCase(route.destinationCampus());
        if (crossCampus) influence += settings.getCrossCampusBonusInfluence();
        if (def.category() == ContractCategory.DIPLOMATIC) {
            influence = (int) Math.round(influence * settings.getDiplomaticContractMultiplier());
        }
        if (def.category() == ContractCategory.EMERGENCY) {
            influence += settings.getEmergencyContractInfluenceBonus();
        }
        if (credited == null) credited = origin;
        if (credited != null) {
            database.logInfluence(credited, new InfluenceRecord(0, def.id(), completerUuid,
                    origin, destination, allianceResolver.apply(origin), allianceResolver.apply(destination),
                    influence, "COMPLETION", Instant.now()));
        }

        String originAlliance = allianceResolver.apply(origin);
        String destinationAlliance = allianceResolver.apply(destination);
        if (originAlliance != null && destinationAlliance != null
                && !originAlliance.equalsIgnoreCase(destinationAlliance)) {
            int bonus = Math.max(1, influence / 2);
            if (credited != null) database.addAlignmentInfluence(credited, bonus);
            database.logInfluence(null, new InfluenceRecord(0, def.id(), completerUuid,
                    origin, destination, originAlliance, destinationAlliance, bonus,
                    "CROSS_ALLIANCE", Instant.now()));
            influence += bonus;
        }
        return influence;
    }

    public int getInfluence(String alignmentId) {
        return alignmentId == null ? 0 : database.getAlignmentInfluence(alignmentId);
    }

    public int getInfluenceBetween(String originAlignmentId, String destinationAlignmentId) {
        if (originAlignmentId == null || destinationAlignmentId == null) return 0;
        return database.getInfluenceBetween(originAlignmentId, destinationAlignmentId);
    }

    public List<InfluenceRecord> getRecentInfluence(int limit) {
        return database.getRecentInfluence(limit);
    }

    /** Admin adjustment; a signed delta applied directly to an alignment total. */
    public void adminAdjustInfluence(String alignmentId, int delta) {
        if (alignmentId != null && delta != 0) database.addAlignmentInfluence(alignmentId, delta);
    }
}
