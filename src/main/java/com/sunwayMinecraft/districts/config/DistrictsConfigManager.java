package com.sunwayMinecraft.districts.config;

import com.sunwayMinecraft.districts.domain.ApprovalBias;
import com.sunwayMinecraft.districts.domain.DistrictAccessRule;
import com.sunwayMinecraft.districts.domain.DistrictControlProfile;
import com.sunwayMinecraft.districts.domain.DistrictDefinition;
import com.sunwayMinecraft.districts.domain.DistrictOwnership;
import com.sunwayMinecraft.districts.domain.DistrictType;
import com.sunwayMinecraft.districts.region.DistrictShape;
import com.sunwayMinecraft.districts.region.Region3i;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Loads districts.yml, including the Triple Alliance ownership and access
 * fields. Malformed entries are skipped with clear log messages instead of
 * aborting the whole reload.
 *
 * <p>Alignment and grand-alliance ids are validated against alignments.yml
 * through a pluggable {@link Predicate}; the default validator reads the id
 * keys from alignments.yml when that file exists. When the alignment system
 * is not configured, ownership references are downgraded to neutral with a
 * warning rather than discarding the district.
 */
public class DistrictsConfigManager {
    private static final String FILE_NAME = "districts.yml";
    private static final String DEFAULT_PROPERTY_POLICY = "default";

    private final JavaPlugin plugin;
    private final Predicate<String> knownAlignmentId;
    private final Map<String, DistrictDefinition> districts = new LinkedHashMap<>();
    private YamlConfiguration config;
    private Set<String> knownPolicyIds = Set.of();
    private final Map<String, DistrictOwnership> ownershipOverrides = new LinkedHashMap<>();
    private final Map<String, DistrictType> typeOverrides = new LinkedHashMap<>();
    private final File overridesFile;
    private final Map<String, DistrictControlProfile> controlProfiles = new LinkedHashMap<>();

    public DistrictsConfigManager(JavaPlugin plugin) {
        this(plugin, defaultAlignmentValidator(plugin));
    }

    public DistrictsConfigManager(JavaPlugin plugin, Predicate<String> knownAlignmentId) {
        this.plugin = plugin;
        this.knownAlignmentId = knownAlignmentId;
        this.overridesFile = new File(plugin.getDataFolder(), "district-overrides.yml");
    }

    /**
     * Default alignment-id validator: reads the grand_alliances and
     * alignments section keys from alignments.yml when that file exists.
     * Kept as a light YAML read so the district system does not depend on
     * the alignment feature being present.
     */
    static Predicate<String> defaultAlignmentValidator(JavaPlugin plugin) {
        return id -> {
            File file = new File(plugin.getDataFolder(), "alignments.yml");
            if (!file.exists()) {
                return false;
            }
            YamlConfiguration alignments = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection grand = alignments.getConfigurationSection("grand_alliances");
            ConfigurationSection aligns = alignments.getConfigurationSection("alignments");
            return (grand != null && grand.isConfigurationSection(id.toLowerCase(Locale.ROOT)))
                    || (aligns != null && aligns.isConfigurationSection(id.toLowerCase(Locale.ROOT)));
        };
    }

    public void reload() {
        ensureDefaultFile();
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        this.config = YamlConfiguration.loadConfiguration(file);
        this.districts.clear();
        loadKnownPolicyIds();

        ConfigurationSection root = config.getConfigurationSection("districts");
        if (root == null) {
            plugin.getLogger().warning("[Districts] No 'districts' section found in " + FILE_NAME);
            return;
        }

        for (String id : root.getKeys(false)) {
            String key = id.toLowerCase(Locale.ROOT);
            if (districts.containsKey(key)) {
                plugin.getLogger().warning("[Districts] Duplicate district id '" + id + "'; later entry skipped.");
                continue;
            }
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) continue;

            try {
                DistrictDefinition definition = parseDistrict(id, key, section);
                districts.put(key, definition);
            } catch (Exception e) {
                plugin.getLogger().warning("[Districts] Failed to load district '" + id + "': " + e.getMessage()
                        + "; entry skipped.");
            }
        }
        applyOverrides();
    }

    private DistrictDefinition parseDistrict(String id, String key, ConfigurationSection section) {
        String displayName = section.getString("display-name", id);
        String shortName = section.getString("short-name", "");
        String world = section.getString("world", "world");
        boolean enabled = section.getBoolean("enabled", true);
        DistrictType type = parseDistrictType(id, section);
        if (type == null) {
            throw new IllegalArgumentException("unknown district-type");
        }
        int prestigeTier = section.getInt("prestige-tier", 1);
        String publicSummary = section.getString("public-summary", "");
        List<String> tags = section.getStringList("tags");
        boolean publicVisible = section.getBoolean("public-visible", true);

        ConfigurationSection listing = section.getConfigurationSection("listing");
        int listingPriority = listing != null ? listing.getInt("priority", 50) : 50;
        boolean storefrontPriority = listing != null && listing.getBoolean("storefront-priority", false);
        boolean residencyPriority = listing != null && listing.getBoolean("residency-priority", false);
        ApprovalBias approvalBias = listing != null
            ? ApprovalBias.fromString(listing.getString("recommended-approval-bias", "STANDARD"))
            : ApprovalBias.STANDARD;

        ConfigurationSection flags = section.getConfigurationSection("flags");
        boolean allowPublicEvents = flags != null && flags.getBoolean("allow-public-events", false);
        boolean signatureArea = flags != null && flags.getBoolean("signature-area", false);

        DistrictShape shape = readShape(world, id, section);
        DistrictOwnership ownership = parseOwnership(id, section);
        controlProfiles.put(key, new DistrictControlProfile(
                section.getBoolean("contest_enabled", false),
                section.getInt("points_required_to_capture", -1),
                section.getDouble("control_point_radius", -1),
                section.getInt("contest_cooldown_seconds", -1)));

        return new DistrictDefinition(
            id,
            displayName,
            blankToNull(shortName),
            world,
            shape,
            enabled,
            type,
            prestigeTier,
            publicSummary,
            tags,
            publicVisible,
            listingPriority,
            storefrontPriority,
            residencyPriority,
            approvalBias,
            allowPublicEvents,
            signatureArea,
            ownership
        );
    }

    private DistrictOwnership parseOwnership(String id, ConfigurationSection section) {
        String homeCampus = firstNonBlank(section.getString("home_campus"), section.getString("home-campus"));
        String grandAllianceOwner = firstNonBlank(
                section.getString("grand_alliance_owner"), section.getString("grand-alliance-owner"));
        String alignmentOwner = firstNonBlank(
                section.getString("alignment_owner"), section.getString("alignment-owner"));
        List<String> allowed = section.getStringList("allowed_alignments");
        List<String> denied = section.getStringList("denied_alignments");
        String propertyPolicy = firstNonBlank(
                section.getString("property_policy"), section.getString("policy-profile"));

        grandAllianceOwner = validateReference(id, "grand_alliance_owner", grandAllianceOwner);
        alignmentOwner = validateReference(id, "alignment_owner", alignmentOwner);
        allowed = validateList(id, "allowed_alignments", allowed);
        denied = validateList(id, "denied_alignments", denied);

        return new DistrictOwnership(
                lowerOrNull(homeCampus),
                lowerOrNull(grandAllianceOwner),
                lowerOrNull(alignmentOwner),
                allowed,
                denied,
                lowerOrNull(propertyPolicy),
                section.getBoolean("transit_connected", section.getBoolean("transit-connected", false)),
                section.getBoolean("contested", false)
        );
    }

    /**
     * Downgrades an owner or list reference that the alignment validator
     * does not recognise: the entry survives as neutral with a logged
     * warning instead of being discarded.
     */
    private String validateReference(String districtId, String field, String reference) {
        if (reference == null || reference.isBlank() || knownAlignmentId.test(reference)) {
            return reference;
        }
        plugin.getLogger().warning("[Districts] District '" + districtId + "' references unknown " + field
                + " '" + reference + "'; treating it as neutral.");
        return null;
    }

    private List<String> validateList(String districtId, String field, List<String> references) {
        if (references == null || references.isEmpty()) {
            return references;
        }
        List<String> validated = new ArrayList<>();
        for (String reference : references) {
            if (knownAlignmentId.test(reference)) {
                validated.add(reference);
            } else {
                plugin.getLogger().warning("[Districts] District '" + districtId + "' lists unknown " + field
                        + " '" + reference + "'; entry ignored.");
            }
        }
        return validated;
    }

    private DistrictType parseDistrictType(String id, ConfigurationSection section) {
        String raw = section.getString("district-type", section.getString("district_type", "MIXED_USE"));
        try {
            return DistrictType.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("[Districts] District '" + id + "' has invalid district-type '"
                    + raw + "'; entry skipped.");
            return null;
        }
    }

    private void loadKnownPolicyIds() {
        File file = new File(plugin.getDataFolder(), "property-policies.yml");
        if (!file.exists()) {
            knownPolicyIds = Set.of();
            return;
        }
        YamlConfiguration policies = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection profiles = policies.getConfigurationSection("policy-profiles");
        knownPolicyIds = profiles == null ? Set.of() : profiles.getKeys(false);
    }

    /**
     * Resolves the property policy id a district references: the
     * {@code property_policy} (or legacy {@code policy-profile}) value when
     * it exists in property-policies.yml, otherwise the default policy with
     * a logged warning.
     */
    public String resolvePropertyPolicyId(DistrictDefinition district) {
        String referenced = district.getOwnership().propertyPolicy();
        if (referenced == null || referenced.isBlank()) {
            return DEFAULT_PROPERTY_POLICY;
        }
        if (knownPolicyIds.isEmpty() || knownPolicyIds.contains(referenced)) {
            return referenced;
        }
        plugin.getLogger().warning("[Districts] District '" + district.getId()
                + "' references missing property policy '" + referenced + "'; falling back to '"
                + DEFAULT_PROPERTY_POLICY + "'.");
        return DEFAULT_PROPERTY_POLICY;
    }

    /** Per-district contest profile; sentinel values mean "use global settings". */
    public DistrictControlProfile getControlProfile(String districtId) {
        return controlProfiles.getOrDefault(districtId.toLowerCase(Locale.ROOT),
                new DistrictControlProfile(false, -1, -1, -1));
    }

    /** Runtime ownership merge used by the control system (not persisted to overrides). */
    public void applyRuntimeOwnership(String districtId, DistrictOwnership ownership) {
        DistrictDefinition existing = districts.get(districtId.toLowerCase(Locale.ROOT));
        if (existing == null || ownership == null) {
            return;
        }
        districts.put(districtId.toLowerCase(Locale.ROOT),
                existing.getDefinitionWith(ownership, existing.getDistrictType()));
    }

    /** Display name of an alignment, read from alignments.yml, or the id. */
    public String alignmentDisplayName(String alignmentId) {
        if (alignmentId == null) {
            return alignmentId;
        }
        File file = new File(plugin.getDataFolder(), "alignments.yml");
        if (!file.exists()) {
            return alignmentId;
        }
        YamlConfiguration alignments = YamlConfiguration.loadConfiguration(file);
        return alignments.getString("alignments." + alignmentId.toLowerCase(Locale.ROOT)
                + ".display_name", alignmentId);
    }

    // ───────────────────── runtime mutations (admin) ─────────────────────

    /** Replaces a district's ownership at runtime. Logs the admin change. */
    public void updateOwnership(String districtId, DistrictOwnership ownership, String actor) {
        DistrictDefinition existing = districts.get(districtId.toLowerCase(Locale.ROOT));
        if (existing == null) {
            throw new IllegalArgumentException("Unknown district: " + districtId);
        }
        districts.put(districtId.toLowerCase(Locale.ROOT),
                existing.getDefinitionWith(ownership, existing.getDistrictType()));
        ownershipOverrides.put(districtId.toLowerCase(Locale.ROOT), ownership);
        plugin.getLogger().info("[Districts] " + actor + " set ownership of '" + districtId + "' to "
                + (ownership.alignmentOwner() != null ? ownership.alignmentOwner() : "neutral"));
    }

    /** Replaces a district's type at runtime. Logs the admin change. */
    public void updateDistrictType(String districtId, DistrictType type, String actor) {
        DistrictDefinition existing = districts.get(districtId.toLowerCase(Locale.ROOT));
        if (existing == null) {
            throw new IllegalArgumentException("Unknown district: " + districtId);
        }
        districts.put(districtId.toLowerCase(Locale.ROOT),
                existing.getDefinitionWith(existing.getOwnership(), type));
        typeOverrides.put(districtId.toLowerCase(Locale.ROOT), type);
        plugin.getLogger().info("[Districts] " + actor + " set type of '" + districtId + "' to " + type);
    }

    /** Persists runtime mutations to district-overrides.yml. */
    public void saveOverrides() {
        YamlConfiguration out = new YamlConfiguration();
        for (Map.Entry<String, DistrictOwnership> entry : ownershipOverrides.entrySet()) {
            DistrictOwnership o = entry.getValue();
            String base = "overrides." + entry.getKey() + ".";
            if (o.alignmentOwner() != null) out.set(base + "alignment_owner", o.alignmentOwner());
            if (o.grandAllianceOwner() != null) out.set(base + "grand_alliance_owner", o.grandAllianceOwner());
            out.set(base + "allowed_alignments", o.allowedAlignments());
            out.set(base + "denied_alignments", o.deniedAlignments());
            if (o.homeCampus() != null) out.set(base + "home_campus", o.homeCampus());
            if (o.propertyPolicy() != null) out.set(base + "property_policy", o.propertyPolicy());
            out.set(base + "transit_connected", o.transitConnected());
            out.set(base + "contested", o.contested());
        }
        for (Map.Entry<String, DistrictType> entry : typeOverrides.entrySet()) {
            out.set("overrides." + entry.getKey() + ".district-type", entry.getValue().name());
        }
        try {
            out.save(overridesFile);
            plugin.getLogger().info("[Districts] Saved " + (ownershipOverrides.size() + typeOverrides.size())
                    + " override(s) to district-overrides.yml");
        } catch (java.io.IOException e) {
            plugin.getLogger().severe("[Districts] Could not save district overrides: " + e.getMessage());
        }
    }

    /** Re-applies saved runtime mutations after districts.yml is parsed. */
    private void applyOverrides() {
        if (!overridesFile.exists()) {
            return;
        }
        YamlConfiguration overrides = YamlConfiguration.loadConfiguration(overridesFile);
        ConfigurationSection root = overrides.getConfigurationSection("overrides");
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            DistrictDefinition existing = districts.get(key);
            if (existing == null) {
                plugin.getLogger().warning("[Districts] Override for unknown district '" + key + "'; ignored.");
                continue;
            }
            ConfigurationSection section = root.getConfigurationSection(key);
            DistrictOwnership current = existing.getOwnership();
            DistrictOwnership ownership = new DistrictOwnership(
                    section.getString("home_campus", current.homeCampus()),
                    section.getString("grand_alliance_owner", current.grandAllianceOwner()),
                    section.getString("alignment_owner", current.alignmentOwner()),
                    section.contains("allowed_alignments")
                            ? section.getStringList("allowed_alignments")
                            : current.allowedAlignments(),
                    section.contains("denied_alignments")
                            ? section.getStringList("denied_alignments")
                            : current.deniedAlignments(),
                    section.getString("property_policy", current.propertyPolicy()),
                    section.getBoolean("transit_connected", current.transitConnected()),
                    section.getBoolean("contested", current.contested()));
            DistrictType type = section.contains("district-type")
                    ? parseTypeSafe(section.getString("district-type"), key)
                    : existing.getDistrictType();
            districts.put(key, existing.getDefinitionWith(ownership, type));
        }
        plugin.getLogger().info("[Districts] Applied district overrides from district-overrides.yml");
    }

    private DistrictType parseTypeSafe(String raw, String key) {
        try {
            return DistrictType.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("[Districts] Invalid overridden district-type for '" + key + "'");
            return districts.containsKey(key) ? districts.get(key).getDistrictType() : DistrictType.MIXED_USE;
        }
    }

    /** Grand alliance id of an alignment, read from alignments.yml, or null. */
    public String allianceOfAlignment(String alignmentId) {
        File file = new File(plugin.getDataFolder(), "alignments.yml");
        if (!file.exists() || alignmentId == null) {
            return null;
        }
        YamlConfiguration alignments = YamlConfiguration.loadConfiguration(file);
        return alignments.getString("alignments." + alignmentId.toLowerCase(Locale.ROOT) + ".grand_alliance");
    }

    private void ensureDefaultFile() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.exists()) {
            plugin.saveResource(FILE_NAME, false);
        }
    }

    public Collection<DistrictDefinition> getDistricts() {
        return Collections.unmodifiableCollection(districts.values());
    }

    public List<DistrictDefinition> getPublicDistricts() {
        List<DistrictDefinition> list = new ArrayList<>();
        for (DistrictDefinition district : districts.values()) {
            if (district.isEnabled() && district.isPublicVisible()) {
                list.add(district);
            }
        }
        list.sort(Comparator.comparingInt(DistrictDefinition::getListingPriority).reversed()
            .thenComparing(DistrictDefinition::getDisplayName, String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    public DistrictDefinition getDistrict(String id) {
        if (id == null) return null;
        return districts.get(id.toLowerCase(Locale.ROOT));
    }

    public DistrictDefinition getDistrictById(String id) {
        return getDistrict(id);
    }

    public List<DistrictDefinition> getDistrictsByCampus(String campusId) {
        List<DistrictDefinition> result = new ArrayList<>();
        if (campusId == null) return result;
        String campus = campusId.toLowerCase(Locale.ROOT);
        for (DistrictDefinition district : districts.values()) {
            if (campus.equals(district.getOwnership().homeCampus())) {
                result.add(district);
            }
        }
        return result;
    }

    public List<DistrictDefinition> getDistrictsByGrandAlliance(String grandAllianceId) {
        List<DistrictDefinition> result = new ArrayList<>();
        if (grandAllianceId == null) return result;
        String alliance = grandAllianceId.toLowerCase(Locale.ROOT);
        for (DistrictDefinition district : districts.values()) {
            if (alliance.equals(district.getOwnership().grandAllianceOwner())) {
                result.add(district);
            }
        }
        return result;
    }

    public List<DistrictDefinition> getDistrictsByType(DistrictType type) {
        List<DistrictDefinition> result = new ArrayList<>();
        if (type == null) return result;
        for (DistrictDefinition district : districts.values()) {
            if (district.getDistrictType() == type) {
                result.add(district);
            }
        }
        return result;
    }

    public List<DistrictDefinition> getContestedDistricts() {
        List<DistrictDefinition> result = new ArrayList<>();
        for (DistrictDefinition district : districts.values()) {
            if (district.getOwnership().contested()) {
                result.add(district);
            }
        }
        return result;
    }

    public List<DistrictDefinition> getTransitDistricts() {
        List<DistrictDefinition> result = new ArrayList<>();
        for (DistrictDefinition district : districts.values()) {
            if (district.getOwnership().transitConnected()) {
                result.add(district);
            }
        }
        return result;
    }

    public List<DistrictDefinition> getEnabledDistricts() {
        List<DistrictDefinition> result = new ArrayList<>();
        for (DistrictDefinition district : districts.values()) {
            if (district.isEnabled()) {
                result.add(district);
            }
        }
        return result;
    }

    public YamlConfiguration getConfig() {
        return config;
    }

    /**
     * Reads the district shape: {@code shape: point_radius} with
     * center_x/y/z and radius, or the classic cuboid {@code region} section.
     * Invalid shapes throw so the entry is skipped with a logged warning.
     */
    private DistrictShape readShape(String world, String id, ConfigurationSection section) {
        String shape = section.getString("shape", "cuboid");
        if ("point_radius".equalsIgnoreCase(shape)) {
            ConfigurationSection center = section.getConfigurationSection("center");
            if (center == null) {
                throw new IllegalArgumentException("point_radius requires a center section");
            }
            double radius = center.getDouble("radius", section.getDouble("radius", 0));
            if (radius <= 0) {
                throw new IllegalArgumentException("point_radius requires a positive radius");
            }
            return DistrictShape.pointRadius(world,
                center.getDouble("x"), center.getDouble("y"), center.getDouble("z"), radius);
        }
        if (!"cuboid".equalsIgnoreCase(shape)) {
            throw new IllegalArgumentException("unknown shape '" + shape + "' (use cuboid or point_radius)");
        }
        ConfigurationSection regionSection = section.getConfigurationSection("region");
        if (regionSection == null) {
            throw new IllegalArgumentException("Missing region section");
        }
        ConfigurationSection min = regionSection.getConfigurationSection("min");
        ConfigurationSection max = regionSection.getConfigurationSection("max");
        if (min == null || max == null) {
            throw new IllegalArgumentException("Region must have min and max");
        }
        return DistrictShape.cuboid(new Region3i(
            world,
            min.getInt("x"),
            min.getInt("y"),
            min.getInt("z"),
            max.getInt("x"),
            max.getInt("y"),
            max.getInt("z")
        ));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String lowerOrNull(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) return primary;
        return fallback;
    }

    static DistrictAccessRule accessRuleFor(DistrictDefinition district) {
        return DistrictAccessRule.forOwnership(district == null ? null : district.getOwnership());
    }
}
