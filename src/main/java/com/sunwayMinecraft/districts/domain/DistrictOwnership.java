package com.sunwayMinecraft.districts.domain;

import java.util.List;
import java.util.Locale;

/**
 * Triple Alliance ownership and access data for a district, loaded from
 * districts.yml. All ids are lowercase alignment/grand-alliance ids as
 * defined in alignments.yml. A district with no owners and no explicit
 * allow/deny lists is neutral.
 *
 * @param homeCampus         campus id this district belongs to, or null
 * @param grandAllianceOwner owning grand alliance id, or null for neutral
 * @param alignmentOwner     owning alignment id, or null for neutral
 * @param allowedAlignments  alignments explicitly allowed; empty means all
 * @param deniedAlignments   alignments explicitly denied; always wins over allowed
 * @param propertyPolicy     referenced property policy id, or null
 * @param transitConnected   whether the district is on the transit network
 * @param contested          whether the district is currently contested
 */
public record DistrictOwnership(
        String homeCampus,
        String grandAllianceOwner,
        String alignmentOwner,
        List<String> allowedAlignments,
        List<String> deniedAlignments,
        String propertyPolicy,
        boolean transitConnected,
        boolean contested) {

    public DistrictOwnership {
        allowedAlignments = lowercase(allowedAlignments);
        deniedAlignments = lowercase(deniedAlignments);
    }

    /** The shipped safe default: neutral, unowned, open to everyone. */
    public static DistrictOwnership neutral() {
        return new DistrictOwnership(null, null, null, List.of(), List.of(), null, false, false);
    }

    /** True when the district has no owner and no explicit access lists. */
    public boolean isNeutral() {
        return grandAllianceOwner == null
                && alignmentOwner == null
                && allowedAlignments.isEmpty()
                && deniedAlignments.isEmpty();
    }

    /** True when an allow/deny list can restrict who enters or acts. */
    public boolean hasAccessRestrictions() {
        return !allowedAlignments.isEmpty() || !deniedAlignments.isEmpty();
    }

    private static List<String> lowercase(List<String> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return ids.stream()
                .filter(id -> id != null && !id.isBlank())
                .map(id -> id.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }
}
