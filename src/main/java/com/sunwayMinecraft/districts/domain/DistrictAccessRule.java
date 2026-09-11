package com.sunwayMinecraft.districts.domain;

import java.util.Locale;

/**
 * Access semantics for a district's ownership data. Rules, in priority order:
 *
 * <ol>
 *   <li>An alignment on {@code denied_alignments} is always denied, even if
 *       also listed in {@code allowed_alignments}.</li>
 *   <li>An empty {@code allowed_alignments} list means every alignment is
 *       allowed.</li>
 *   <li>Otherwise only listed alignments are allowed.</li>
 * </ol>
 *
 * Players without an alignment follow the unaligned rule: they are allowed
 * wherever alignments in general are allowed (empty allow list), and denied
 * wherever an explicit allow list restricts the district.
 */
public record DistrictAccessRule(DistrictOwnership ownership) {

    public DistrictAccessRule {
        java.util.Objects.requireNonNull(ownership, "ownership");
    }

    /** Rule for a district's ownership data; null ownership means neutral. */
    public static DistrictAccessRule forOwnership(DistrictOwnership ownership) {
        return new DistrictAccessRule(ownership == null ? DistrictOwnership.neutral() : ownership);
    }

    /** Whether the given alignment may access the district. */
    public boolean allows(String alignmentId) {
        if (alignmentId == null) {
            return allowsUnaligned();
        }
        String id = alignmentId.trim().toLowerCase(Locale.ROOT);
        if (ownership.deniedAlignments().contains(id)) {
            return false;
        }
        return ownership.allowedAlignments().isEmpty()
                || ownership.allowedAlignments().contains(id);
    }

    /** Whether a player without any alignment may access the district. */
    public boolean allowsUnaligned() {
        return ownership.allowedAlignments().isEmpty();
    }
}
