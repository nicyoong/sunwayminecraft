package com.sunwayMinecraft.residency.config;

import com.sunwayMinecraft.residency.domain.BillingPeriod;
import com.sunwayMinecraft.residency.domain.PricingProfile;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Map;

public class PricingConfigManager extends AbstractYamlConfigManager {
    private final Map<String, PricingProfile> profiles = new LinkedHashMap<>();

    public PricingConfigManager(JavaPlugin plugin) { super(plugin, "property-pricing.yml"); }

    @Override
    protected void load() {
        profiles.clear();
        ConfigurationSection root = config.getConfigurationSection("pricing-profiles");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) continue;
            profiles.put(id.toLowerCase(), new PricingProfile(
                    id,
                    BillingPeriod.valueOf(s.getString("billing-period", "MONTHLY").toUpperCase()),
                    s.getDouble("base-rent", 0.0),
                    s.getDouble("deposit", 0.0),
                    s.getBoolean("late-fee-enabled", false),
                    s.getDouble("late-fee", 0.0)
            ));
        }
    }

    public PricingProfile getProfile(String id) { return profiles.get(id.toLowerCase()); }
}
