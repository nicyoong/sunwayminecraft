package com.sunwayMinecraft.alignments.domain;

/** A single alignment a player may join, loaded from alignments.yml. */
public record AlignmentDefinition(
    String id,
    String displayName,
    GrandAlliance grandAlliance,
    Campus homeCampus,
    String description,
    String chatPrefix,
    String chatColor,
    boolean enabled) {}
