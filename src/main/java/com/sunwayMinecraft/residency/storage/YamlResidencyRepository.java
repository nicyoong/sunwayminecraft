package com.sunwayMinecraft.residency.storage;

import com.sunwayMinecraft.residency.domain.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
        import java.util.stream.Collectors;

public class YamlResidencyRepository implements ResidencyRepository {
    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration yaml;
    private final Map<String, UnitTenancyRecord> tenancies = new LinkedHashMap<>();
    private final Map<String, List<RoleAssignment>> roles = new LinkedHashMap<>();
    private final Map<String, List<GuestAccessGrant>> guests = new LinkedHashMap<>();
    private final Map<String, EscrowRecord> escrows = new LinkedHashMap<>();

    public YamlResidencyRepository(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "residency-data.yml");
    }

    @Override
    public void load() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        yaml = YamlConfiguration.loadConfiguration(file);
        tenancies.clear(); roles.clear(); guests.clear(); escrows.clear();
        ConfigurationSection tRoot = yaml.getConfigurationSection("tenancies");
        if (tRoot != null) {
            for (String unitId : tRoot.getKeys(false)) {
                ConfigurationSection s = tRoot.getConfigurationSection(unitId);
                UnitTenancyRecord rec = new UnitTenancyRecord(unitId);
                rec.setLeaseState(LeaseState.valueOf(s.getString("lease-state", "VACANT")));
                rec.setRentState(RentState.valueOf(s.getString("rent-state", "CLOSED")));
                String tenant = s.getString("tenant");
                if (tenant != null && !tenant.isBlank()) rec.setTenantPlayerId(UUID.fromString(tenant));
                for (String manager : s.getStringList("managers")) rec.getManagerIds().add(UUID.fromString(manager));
                rec.setLeaseStart(readInstant(s, "lease-start"));
                rec.setLeaseEnd(readInstant(s, "lease-end"));
                rec.setGraceEnd(readInstant(s, "grace-end"));
                rec.setDepositAmount(s.getDouble("deposit-amount", 0.0));
                rec.setRentAmount(s.getDouble("rent-amount", 0.0));
                String billing = s.getString("billing-period");
                if (billing != null && !billing.isBlank()) rec.setBillingPeriod(BillingPeriod.valueOf(billing));
                rec.setArrearsAmount(s.getDouble("arrears-amount", 0.0));
                rec.setLastPaymentAt(readInstant(s, "last-payment-at"));
                rec.setNextDueAt(readInstant(s, "next-due-at"));
                rec.setApprovalRequired(s.getBoolean("approval-required", false));
                tenancies.put(unitId.toLowerCase(), rec);
            }
        }
    }
