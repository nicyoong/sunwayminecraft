package com.sunwayMinecraft.alignments.domain;

import java.util.UUID;

/**
 * A player's persisted alignment membership. Reputation is stored for future
 * parts of the Triple Alliance expansion but has no gameplay effect yet.
 */
public record AlignmentMembership(
    UUID playerUuid, String alignmentId, long joinedAt, int reputation, String status) {

  public static final String STATUS_ACTIVE = "active";

  public static AlignmentMembership newMembership(UUID playerUuid, String alignmentId, long joinedAt) {
    return new AlignmentMembership(playerUuid, alignmentId, joinedAt, 0, STATUS_ACTIVE);
  }

  public boolean isSameAlignment(String otherAlignmentId) {
    return alignmentId != null && alignmentId.equalsIgnoreCase(otherAlignmentId);
  }
}
