package com.sunwayMinecraft.regions;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.events.ClaimCreatedEvent;
import me.ryanhamshire.GriefPrevention.events.ClaimDeletedEvent;
import me.ryanhamshire.GriefPrevention.events.ClaimModifiedEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class GPListener implements Listener{
    private final JavaPlugin plugin;
    private final RegionManager regionManager;

    public GPListener(JavaPlugin plugin, RegionManager regionManager) {
        this.plugin = plugin;
        this.regionManager = regionManager;
    }

    @EventHandler
    public void onClaimCreated(ClaimCreatedEvent event) {
        Claim claim = event.getClaim();
        // Skip if region already exists for this claim
        if (regionManager.getRegionByClaimId(claim.getID()) != null) return;

        // Create new region linked to claim
        String world = claim.getLesserBoundaryCorner().getWorld().getName();
        regionManager.createRegion(
                "GP_" + claim.getID(),
                world,
                claim.getLesserBoundaryCorner().getBlockX(),
                claim.getLesserBoundaryCorner().getBlockY(),
                claim.getLesserBoundaryCorner().getBlockZ(),
                claim.getGreaterBoundaryCorner().getBlockX(),
                claim.getGreaterBoundaryCorner().getBlockY(),
                claim.getGreaterBoundaryCorner().getBlockZ(),
                claim.getID(),
                false
        );
    }

    @EventHandler
    public void onClaimModified(ClaimModifiedEvent event) {
        Claim claim = event.getClaim();
        Region region = regionManager.getRegionByClaimId(claim.getID());

        // Only update if region exists and is still linked to GP
        if (region != null && !region.isDecoupled()) {
            regionManager.updateRegionBounds(
                    region.getName(),
                    claim.getLesserBoundaryCorner().getBlockX(),
                    claim.getLesserBoundaryCorner().getBlockY(),
                    claim.getLesserBoundaryCorner().getBlockZ(),
                    claim.getGreaterBoundaryCorner().getBlockX(),
                    claim.getGreaterBoundaryCorner().getBlockY(),
                    claim.getGreaterBoundaryCorner().getBlockZ()
            );
        }
    }

    @EventHandler
    public void onClaimDeleted(ClaimDeletedEvent event) {
        Claim claim = event.getClaim();
        Region region = regionManager.getRegionByClaimId(claim.getID());

        // Only delete if region exists and is still linked to GP
        if (region != null && !region.isDecoupled()) {
            regionManager.deleteRegion(region.getName());
        }
    }
}
