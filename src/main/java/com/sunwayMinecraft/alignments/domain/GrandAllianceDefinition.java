package com.sunwayMinecraft.alignments.domain;

/**
 * Display metadata for a grand alliance, loaded from alignments.yml. The enum
 * {@link GrandAlliance} carries the stable id; this record carries what players see.
 */
public record GrandAllianceDefinition(
    String id, String displayName, String description, String chatColor) {}
