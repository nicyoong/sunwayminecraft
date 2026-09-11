package com.sunwayMinecraft.districts.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Access semantics: denied wins over allowed, empty allow = everyone, unaligned. */
class DistrictAccessRuleTest {

    private DistrictOwnership ownershipWith(List<String> allowed, List<String> denied) {
        return new DistrictOwnership(null, null, null, allowed, denied, null, false, false);
    }

    @Test
    void neutralOwnershipAllowsEveryone() {
        DistrictAccessRule rule = DistrictAccessRule.forOwnership(DistrictOwnership.neutral());

        assertTrue(rule.allows("azure_hearth"));
        assertTrue(rule.allows("spirewrights"));
        assertTrue(rule.allowsUnaligned());
    }

    @Test
    void deniedAlignmentIsDeniedEvenWhenAlsoAllowed() {
        DistrictAccessRule rule = new DistrictAccessRule(ownershipWith(
                List.of("azure_hearth", "spirewrights"), List.of("spirewrights")));

        assertFalse(rule.allows("spirewrights"), "denied must win over allowed");
        assertTrue(rule.allows("azure_hearth"));
    }

    @Test
    void emptyAllowedListMeansAllAlignmentsAreAllowed() {
        DistrictAccessRule rule = new DistrictAccessRule(ownershipWith(
                List.of(), List.of()));

        assertTrue(rule.allows("azure_hearth"));
        assertTrue(rule.allows("anything_at_all"));
        assertTrue(rule.allowsUnaligned());
    }

    @Test
    void nonEmptyAllowedListIsAWhitelist() {
        DistrictAccessRule rule = new DistrictAccessRule(ownershipWith(
                List.of("azure_hearth"), List.of()));

        assertTrue(rule.allows("azure_hearth"));
        assertFalse(rule.allows("lagoon_covenant"));
        assertFalse(rule.allowsUnaligned(),
                "unaligned players are denied where an explicit allow list restricts access");
    }

    @Test
    void alignmentIdsAreNormalisedCaseInsensitively() {
        DistrictAccessRule rule = new DistrictAccessRule(ownershipWith(
                List.of("azure_hearth"), List.of("spirewrights")));

        assertTrue(rule.allows("AZURE_HEARTH"));
        assertFalse(rule.allows("Spirewrights"));
    }

    @Test
    void nullOwnershipIsTreatedAsNeutral() {
        DistrictAccessRule rule = DistrictAccessRule.forOwnership(null);

        assertTrue(rule.allows("azure_hearth"));
        assertTrue(rule.allowsUnaligned());
    }
}
