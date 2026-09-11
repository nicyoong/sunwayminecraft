package com.sunwayMinecraft.alignments.domain;

import java.util.Arrays;
import java.util.Optional;

/** Stable ids for the two grand alliances of the Triple Alliance. */
public enum GrandAlliance {
  CONCORDAT_OF_THE_DAWN("concordat_of_the_dawn"),
  IRONCLAD_SYNDICATE("ironclad_syndicate");

  private final String id;

  GrandAlliance(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  public static Optional<GrandAlliance> fromId(String id) {
    if (id == null) return Optional.empty();
    return Arrays.stream(values())
        .filter(alliance -> alliance.id.equalsIgnoreCase(id.trim()))
        .findFirst();
  }
}
