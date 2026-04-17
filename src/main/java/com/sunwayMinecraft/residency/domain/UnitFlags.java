package com.sunwayMinecraft.residency.domain;

public class UnitFlags {
    private final boolean publicEntry;
    private final boolean allowGuests;
    private final boolean allowEventAccess;
    private final boolean escrowOnRepossession;
    private final boolean publicInteraction;
    private final boolean publicContainerAccess;

    public UnitFlags(boolean publicEntry, boolean allowGuests, boolean allowEventAccess, boolean escrowOnRepossession,
                     boolean publicInteraction, boolean publicContainerAccess) {
        this.publicEntry = publicEntry;
        this.allowGuests = allowGuests;
        this.allowEventAccess = allowEventAccess;
        this.escrowOnRepossession = escrowOnRepossession;
        this.publicInteraction = publicInteraction;
        this.publicContainerAccess = publicContainerAccess;
    }

    public boolean isPublicEntry() { return publicEntry; }
    public boolean isAllowGuests() { return allowGuests; }
    public boolean isAllowEventAccess() { return allowEventAccess; }
    public boolean isEscrowOnRepossession() { return escrowOnRepossession; }
    public boolean isPublicInteraction() { return publicInteraction; }
    public boolean isPublicContainerAccess() { return publicContainerAccess; }
}
