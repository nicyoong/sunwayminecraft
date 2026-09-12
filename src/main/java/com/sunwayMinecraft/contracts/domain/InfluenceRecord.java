package com.sunwayMinecraft.contracts.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * One row of the contract influence ledger: who completed what, the
 * alignment and grand-alliance pairing it shifted, the signed influence
 * amount and the kind of award (COMPLETION, CROSS_ALLIANCE, ADMIN,
 * SABOTAGE, ...).
 */
public record InfluenceRecord(
    long id,
    String contractId,
    UUID playerUuid,
    String originAlignmentId,
    String destinationAlignmentId,
    String originGrandAllianceId,
    String destinationGrandAllianceId,
    int influenceAmount,
    String influenceType,
    Instant createdAt
) {
}
