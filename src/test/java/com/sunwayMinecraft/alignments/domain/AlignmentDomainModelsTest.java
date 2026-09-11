package com.sunwayMinecraft.alignments.domain;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Edge cases of the stable-id enums and membership helpers (Part 1 models). */
class AlignmentDomainModelsTest {
    @Test
    void grandAllianceIdsResolveCaseInsensitivelyAndRejectUnknowns() {
        assertEquals(Optional.of(GrandAlliance.IRONCLAD_SYNDICATE),
                GrandAlliance.fromId("IRONCLAD_SYNDICATE"));
        assertEquals(Optional.of(GrandAlliance.CONCORDAT_OF_THE_DAWN),
                GrandAlliance.fromId("  concordat_of_the_dawn  "));
        assertTrue(GrandAlliance.fromId("not_an_alliance").isEmpty());
        assertTrue(GrandAlliance.fromId(null).isEmpty());
        assertTrue(GrandAlliance.fromId("").isEmpty());
    }

    @Test
    void campusIdsResolveCaseInsensitivelyAndRejectUnknowns() {
        assertEquals(Optional.of(Campus.MONASH), Campus.fromId("Monash"));
        assertTrue(Campus.fromId("harvard").isEmpty());
        assertTrue(Campus.fromId(null).isEmpty());
    }

    @Test
    void membershipComparisonIsCaseInsensitiveAndNullSafe() {
        AlignmentMembership membership = AlignmentMembership.newMembership(
                java.util.UUID.randomUUID(), "azure_hearth", 1000L);

        assertTrue(membership.isSameAlignment("AZURE_HEARTH"));
        assertTrue(membership.isSameAlignment("azure_hearth"));
        org.junit.jupiter.api.Assertions.assertFalse(membership.isSameAlignment("spirewrights"));
        org.junit.jupiter.api.Assertions.assertFalse(membership.isSameAlignment(null));
    }

    @Test
    void newMembershipStartsActiveWithZeroReputation() {
        AlignmentMembership membership = AlignmentMembership.newMembership(
                java.util.UUID.randomUUID(), "lagoon_covenant", 42L);
        assertEquals("active", membership.status());
        assertEquals(0, membership.reputation());
        assertEquals(42L, membership.joinedAt());
    }
}
