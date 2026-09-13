package com.sunwayMinecraft.districts.service;

import com.sunwayMinecraft.districts.config.DistrictSettingsConfig;
import com.sunwayMinecraft.districts.domain.DistrictDefinition;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Guard that the residency rental flow calls before starting a lease. The
 * district system initializes after residency, so all dependencies are
 * late-bound through suppliers.
 */
public class DistrictResidencyGuard {
    /** Enforcement counter hook (wired to the city metrics manager). */
    public interface EnforcementMetrics {
        void increment(String key);
    }

    private final Supplier<DistrictAlignmentService> alignmentService;
    private final Supplier<DistrictSettingsConfig> settings;
    private final Supplier<EnforcementMetrics> metrics;

    public DistrictResidencyGuard(
            Supplier<DistrictAlignmentService> alignmentService,
            Supplier<DistrictSettingsConfig> settings,
            Supplier<EnforcementMetrics> metrics) {
        this.alignmentService = alignmentService;
        this.settings = settings;
        this.metrics = metrics;
    }

    /**
     * Rental denial reason for a unit in the given district, or empty when
     * the rental may proceed.
     */
    public Optional<String> checkRentalAllowed(Player player, String districtId) {
        DistrictSettingsConfig effectiveSettings = settingsSupplierSafe();
        if (effectiveSettings == null || !effectiveSettings.isEnforceResidencyRules()) {
            return Optional.empty();
        }
        DistrictAlignmentService alignment = alignmentService.get();
        if (alignment == null) {
            return Optional.empty();
        }
        DistrictDefinition district = alignment.getDistrictById(districtId);
        if (district == null) {
            return Optional.empty(); // unit sits outside known districts
        }
        if (district.isArchived() && effectiveSettings.isArchivedReadOnly()) {
            return Optional.of("This unit is inside a read-only archive district.");
        }
        if (district.getOwnership().contested() && !effectiveSettings.isContestedAllowAccess()) {
            return Optional.of("This district is contested - rentals are paused.");
        }
        if (!alignment.canPlayerAccessDistrict(player, district)) {
            metrics().ifPresent(m -> m.increment("district_rent_denied"));
            return Optional.of("This district is controlled by another alignment.");
        }
        return Optional.empty();
    }

    private DistrictSettingsConfig settingsSupplierSafe() {
        try {
            return settings.get();
        } catch (NullPointerException | IllegalStateException e) {
            return null; // district system not initialized yet
        }
    }

    private Optional<EnforcementMetrics> metrics() {
        try {
            return Optional.ofNullable(metrics.get());
        } catch (NullPointerException | IllegalStateException e) {
            return Optional.empty();
        }
    }
}
