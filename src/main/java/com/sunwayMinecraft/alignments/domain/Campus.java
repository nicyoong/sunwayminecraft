package com.sunwayMinecraft.alignments.domain;

import java.util.Arrays;
import java.util.Optional;

/** Stable ids for the three campuses of the Triple Alliance. */
public enum Campus {
  TAYLORS("taylors"),
  SUNWAY("sunway"),
  MONASH("monash");

  private final String id;

  Campus(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  public static Optional<Campus> fromId(String id) {
    if (id == null) return Optional.empty();
    return Arrays.stream(values())
        .filter(campus -> campus.id.equalsIgnoreCase(id.trim()))
        .findFirst();
  }
}
