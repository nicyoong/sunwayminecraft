package com.sunwayMinecraft.contracts.domain;

/** Campus logistics metadata for a contract: where it runs and who ships it. */
public record ContractCampusRoute(
    String originCampus,
    String destinationCampus,
    String originAlignment,
    String destinationAlignment
) {
  public static final ContractCampusRoute NONE =
      new ContractCampusRoute(null, null, null, null);

  /** True when the route starts at, ends at, or passes through the campus. */
  public boolean touchesCampus(String campusId) {
    return campusId != null
        && (campusId.equalsIgnoreCase(originCampus)
            || campusId.equalsIgnoreCase(destinationCampus));
  }

  public boolean isBetween(String originCampus, String destinationCampus) {
    return originCampus != null && destinationCampus != null
        && originCampus.equalsIgnoreCase(this.originCampus)
        && destinationCampus.equalsIgnoreCase(this.destinationCampus);
  }
}
