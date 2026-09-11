package com.sunwayMinecraft.districts.config;

import com.sunwayMinecraft.districts.domain.ApprovalBias;
import com.sunwayMinecraft.districts.domain.DistrictAccessRule;
import com.sunwayMinecraft.districts.domain.DistrictDefinition;
import com.sunwayMinecraft.districts.domain.DistrictOwnership;
import com.sunwayMinecraft.districts.domain.DistrictType;
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

    public DistrictsConfigManager(JavaPlugin plugin) {
        this(plugin, defaultAlignmentValidator(plugin));
    }

    public DistrictsConfigManager(JavaPlugin plugin, Predicate<String> knownAlignmentId) {
        this.plugin = plugin;
        this.knownAlignmentId = knownAlignmentId;
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

        Region3i region = readRegion(world, section.getConfigurationSection("region"));
        DistrictOwnership ownership = parseOwnership(id, section);

        return new DistrictDefinition(
            id,
            displayName,
            blankToNull(shortName),
            world,
            region,
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

    public YamlConfiguration getConfig() {
        return config;
    }

    private Region3i readRegion(String world, ConfigurationSection section) {
        if (section == null) {
            throw new IllegalArgumentException("Missing region section");
        }
        ConfigurationSection min = section.getConfigurationSection("min");
        ConfigurationSection max = section.getConfigurationSection("max");
        if (min == null || max == null) {
            throw new IllegalArgumentException("Region must have min and max");
        }
        return new Region3i(
            world,
            min.getInt("x"),
            min.getInt("y"),
            min.getInt("z"),
            max.getInt("x"),
            max.getInt("y"),
            max.getInt("z")
        );
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
