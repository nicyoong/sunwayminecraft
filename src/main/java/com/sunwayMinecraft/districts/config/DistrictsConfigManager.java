package com.sunwayMinecraft.districts.config;

import com.sunwayMinecraft.districts.domain.ApprovalBias;
import com.sunwayMinecraft.districts.domain.DistrictDefinition;
import com.sunwayMinecraft.districts.domain.DistrictType;
import com.sunwayMinecraft.districts.region.Region3i;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

public class DistrictsConfigManager {
    private static final String FILE_NAME = "districts.yml";
    private final JavaPlugin plugin;
    private final Map<String, DistrictDefinition> districts = new LinkedHashMap<>();
    private YamlConfiguration config;

    public DistrictsConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        ensureDefaultFile();
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        this.config = YamlConfiguration.loadConfiguration(file);
        this.districts.clear();

        ConfigurationSection root = config.getConfigurationSection("districts");
        if (root == null) {
            plugin.getLogger().warning("[Districts] No 'districts' section found in " + FILE_NAME);
            return;
        }

        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) continue;

            String displayName = section.getString("display-name", id);
            String shortName = section.getString("short-name", "");
            String world = section.getString("world", "world");
            boolean enabled = section.getBoolean("enabled", true);
            DistrictType type = DistrictType.valueOf(section.getString("district-type", "MIXED_USE").toUpperCase(Locale.ROOT));
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

            DistrictDefinition definition = new DistrictDefinition(
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
                signatureArea
            );

            districts.put(id.toLowerCase(Locale.ROOT), definition);
        }
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

