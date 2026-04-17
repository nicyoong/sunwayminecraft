package com.sunwayMinecraft.residency.domain;

import java.util.Collections;
import java.util.List;

public class ListingSettings {
    private final boolean visible;
    private final boolean approvalRequired;
    private final List<String> tags;

    public ListingSettings(boolean visible, boolean approvalRequired, List<String> tags) {
        this.visible = visible;
        this.approvalRequired = approvalRequired;
        this.tags = tags == null ? Collections.emptyList() : Collections.unmodifiableList(tags);
    }

    public boolean isVisible() { return visible; }
    public boolean isApprovalRequired() { return approvalRequired; }
    public List<String> getTags() { return tags; }
}
