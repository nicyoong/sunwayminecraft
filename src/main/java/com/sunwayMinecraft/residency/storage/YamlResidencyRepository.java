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
