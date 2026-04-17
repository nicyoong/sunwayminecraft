package com.sunwayMinecraft.residency.config;

import com.sunwayMinecraft.residency.domain.PolicyProfile;
import com.sunwayMinecraft.residency.domain.UnitMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Map;

public class PolicyConfigManager extends AbstractYamlConfigManager {
    private final Map<String, PolicyProfile> profiles = new LinkedHashMap<>();

    public PolicyConfigManager(JavaPlugin plugin) { super(plugin, "property-policies.yml"); }

    @Override
    protected void load() {
        profiles.clear();
        ConfigurationSection root = config.getConfigurationSection("policy-profiles");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) continue;
            profiles.put(id.toLowerCase(), new PolicyProfile(
                    id,
                    UnitMode.valueOf(s.getString("default-role-preset", "RESIDENTIAL").toUpperCase()),
                    s.getInt("rent-grace-days", 7),
                    s.getBoolean("repossession-enabled", true),
                    s.getBoolean("auto-restrict-on-arrears", true),
                    s.getBoolean("allow-public-container-access", false),
                    s.getBoolean("allow-public-interaction", false)
            ));
        }
    }

    public PolicyProfile getProfile(String id) { return profiles.get(id.toLowerCase()); }
}
