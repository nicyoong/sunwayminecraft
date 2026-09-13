package com.sunwayMinecraft.contracts.service;

import com.sunwayMinecraft.contracts.config.ContractConfigManager;
import com.sunwayMinecraft.contracts.config.ContractDiplomacySettings;
import com.sunwayMinecraft.contracts.config.ContractTemplateConfigManager;
import com.sunwayMinecraft.contracts.domain.ContractCategory;
import com.sunwayMinecraft.contracts.domain.ContractDefinition;
import com.sunwayMinecraft.contracts.domain.ContractEndpoint;
import com.sunwayMinecraft.contracts.persistence.ContractDatabase;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Generates short-lived emergency contracts from templates for the dynamic
 * layer: an admin command, the scheduled interval, or an optional trigger hook
 * (district contest / treasury) that calls {@link #generate} directly. Generated
 * contracts reuse existing endpoints, are always EMERGENCY (so they earn the
 * influence bonus and surface under {@code /contracts board emergency}), are
 * dropped if their endpoints are invalid, respect a live cap, and persist across
 * restarts in SQLite.
 */
public class DynamicContractService {
    private final ContractConfigManager contractConfig;
    private final ContractTemplateConfigManager templates;
    private final com.sunwayMinecraft.contracts.config.EndpointConfigManager endpointConfig;
    private final ContractDatabase database;
    private final ContractDiplomacySettings settings;
    private final JavaPlugin plugin;
    private final Set<String> liveIds = new LinkedHashSet<>();

    public DynamicContractService(ContractConfigManager contractConfig,
                                  ContractTemplateConfigManager templates,
                                  com.sunwayMinecraft.contracts.config.EndpointConfigManager endpointConfig,
                                  ContractDatabase database, ContractDiplomacySettings settings,
                                  JavaPlugin plugin) {
        this.contractConfig = contractConfig;
        this.templates = templates;
        this.endpointConfig = endpointConfig;
        this.database = database;
        this.settings = settings;
        this.plugin = plugin;
    }

    /** Generates one instance of a template; returns its unique id, or empty if refused. */
    public Optional<String> generate(String templateId) {
        ContractDefinition template = templates.getTemplate(templateId);
        if (template == null) {
            log("Unknown emergency template: " + templateId);
            return Optional.empty();
        }
        if (liveIds.size() >= settings.getMaxActiveDynamicContracts()) {
            log("Dynamic contract cap reached; refusing " + templateId);
            return Optional.empty();
        }
        if (hasMissingEndpoint(template)) {
            log("Template " + templateId + " references a missing endpoint; not generated.");
            return Optional.empty();
        }
        String id = "dyn_" + template.id() + "_" + System.nanoTime();
        ContractDefinition generated = withId(template, id);
        contractConfig.addRuntimeContract(generated);
        long expiresAt = Instant.now().plus(Duration.ofMinutes(Math.max(1, template.durationMinutes())))
                .toEpochMilli();
        database.saveDynamicContract(id, template.id(), expiresAt);
        liveIds.add(id);
        log("Generated dynamic contract " + id + " from template " + template.id());
        return Optional.of(id);
    }

    /** Rebuilds non-expired generated contracts from SQLite at startup. */
    public void loadPersisted() {
        for (ContractDatabase.DynamicContractRow row : database.loadDynamicContracts(System.currentTimeMillis())) {
            ContractDefinition template = templates.getTemplate(row.templateId());
            if (template == null || hasMissingEndpoint(template)) {
                database.deleteDynamicContract(row.contractId());
                continue;
            }
            contractConfig.addRuntimeContract(withId(template, row.contractId()));
            liveIds.add(row.contractId());
        }
    }

    /** Drops runtime contracts whose generation window has passed. */
    public void expireOverdue() {
        long now = System.currentTimeMillis();
        Set<String> stillLive = new java.util.HashSet<>();
        for (ContractDatabase.DynamicContractRow row : database.loadDynamicContracts(now)) {
            stillLive.add(row.contractId());
        }
        for (String id : new LinkedHashSet<>(liveIds)) {
            if (!stillLive.contains(id)) {
                contractConfig.removeRuntimeContract(id);
                liveIds.remove(id);
                log("Dynamic contract " + id + " expired.");
            }
        }
        database.purgeExpiredDynamicContracts(now);
    }

    /** Admin removal of every live dynamic contract. */
    public int clearAll() {
        int removed = 0;
        for (String id : new LinkedHashSet<>(liveIds)) {
            contractConfig.removeRuntimeContract(id);
            database.deleteDynamicContract(id);
            removed++;
        }
        liveIds.clear();
        return removed;
    }

    public Set<String> liveIds() { return liveIds; }

    private boolean hasMissingEndpoint(ContractDefinition def) {
        return def.startEndpointId() == null
                || endpointConfig.getEndpoint(def.startEndpointId()) == null
                || def.endEndpointId() == null
                || endpointConfig.getEndpoint(def.endEndpointId()) == null;
    }

    private ContractDefinition withId(ContractDefinition template, String id) {
        return new ContractDefinition(id, ContractCategory.EMERGENCY, template.name(),
                template.description(), template.rewardMoney(), template.durationMinutes(),
                template.cooldownMinutes(), template.startEndpointId(), template.endEndpointId(),
                template.requiredMaterials(), template.objectiveDescription(), template.objectiveType(),
                template.alignmentRule(), template.campusRoute(), template.rewardReputation(), true);
    }

    private void log(String message) {
        if (plugin != null && plugin.getLogger() != null) {
            plugin.getLogger().info("[Contracts] " + message);
        }
    }
}
