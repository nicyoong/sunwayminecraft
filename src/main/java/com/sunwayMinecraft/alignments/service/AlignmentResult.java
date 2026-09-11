package com.sunwayMinecraft.alignments.service;

/** Outcome of an alignment join/leave attempt. */
public enum AlignmentResult {
  JOINED(true),
  LEFT(true),
  ALREADY_ALIGNED(false),
  NOT_ALIGNED(false),
  NOT_FOUND(false),
  DISABLED(false),
  DATABASE_FAILURE(false);

  private final boolean success;

  AlignmentResult(boolean success) {
    this.success = success;
  }

  public boolean isSuccess() {
    return success;
  }
}
