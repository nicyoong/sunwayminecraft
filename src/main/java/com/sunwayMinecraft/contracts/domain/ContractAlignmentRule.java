package com.sunwayMinecraft.contracts.domain;

import java.util.List;
import java.util.Locale;

/**
 * Alignment gating parsed from a contract entry. A null requiredAlignment opens
 * the contract to every alignment; a forbidden alignment is always refused.
 */
public record ContractAlignmentRule(
    String requiredAlignment,
    String recommendedAlignment,
    List<String> forbiddenAlignments
) {
  public static final ContractAlignmentRule OPEN =
      new ContractAlignmentRule(null, null, List.of());

  public ContractAlignmentRule {
    forbiddenAlignments =
        forbiddenAlignments == null ? List.of() : List.copyOf(forbiddenAlignments);
  }

  /** True when the alignment (or an unaligned player when nothing is required) may accept. */
  public boolean canAccept(String alignmentId) {
    if (alignmentId != null
        && forbiddenAlignments.contains(alignmentId.toLowerCase(Locale.ROOT))) {
      return false;
    }
    return requiredAlignment == null
        || (alignmentId != null && alignmentId.equalsIgnoreCase(requiredAlignment));
  }

  public boolean isOpen() {
    return requiredAlignment == null && forbiddenAlignments.isEmpty();
  }
}
