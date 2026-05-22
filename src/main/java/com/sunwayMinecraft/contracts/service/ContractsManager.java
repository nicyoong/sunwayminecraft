package com.sunwayMinecraft.contracts.service;

import com.sunwayMinecraft.city.metrics.CityMetricKeys;
import com.sunwayMinecraft.city.metrics.CityMetricsManager;
import com.sunwayMinecraft.contracts.config.ContractConfigManager;
import com.sunwayMinecraft.contracts.config.EndpointConfigManager;
import com.sunwayMinecraft.contracts.config.SettingsConfigManager;
import com.sunwayMinecraft.contracts.domain.*;
import com.sunwayMinecraft.contracts.persistence.ContractPersistenceService;
import com.sunwayMinecraft.events.service.EventModifierService;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ContractsManager {
    private final JavaPlugin plugin;
    private final ContractConfigManager contractConfig;
    private final EndpointConfigManager endpointConfig;
    private final SettingsConfigManager settingsConfig;
    private final ContractPersistenceService persistence;
    private final Economy economy;
    private EventModifierService eventModifierService;
    private CityMetricsManager metricsManager;

    public ContractsManager(JavaPlugin plugin, ContractConfigManager contractConfig, 
                            EndpointConfigManager endpointConfig, SettingsConfigManager settingsConfig,
                            ContractPersistenceService persistence, Economy economy) {
        this.plugin = plugin;
        this.contractConfig = contractConfig;
        this.endpointConfig = endpointConfig;
        this.settingsConfig = settingsConfig;
        this.persistence = persistence;
        this.economy = economy;
    }

    public void setEventModifierService(EventModifierService eventModifierService) {
        this.eventModifierService = eventModifierService;
    }

    public void setMetricsManager(CityMetricsManager metricsManager) {
        this.metricsManager = metricsManager;
    }

    public boolean acceptContract(Player player, String contractId) {
        ContractDefinition def = contractConfig.getContract(contractId);
        if (def == null) return false;

        UUID uuid = player.getUniqueId();
        List<ActiveContract> active = persistence.getPlayerContracts(uuid);
        
        if (active.size() >= settingsConfig.getMaxActiveContracts()) return false;
        
        // Check cooldown
        Instant cooldownUntil = persistence.getPlayerCooldowns(uuid).get(contractId);
        if (cooldownUntil != null && Instant.now().isBefore(cooldownUntil)) return false;

        Instant expiry = Instant.now().plus(Duration.ofMinutes(def.durationMinutes()));
        ActiveContract newContract = new ActiveContract(uuid, contractId, Instant.now(), expiry);
        active.add(newContract);
        persistence.save();
        
        if (metricsManager != null) {
            metricsManager.increment(CityMetricKeys.CONTRACTS_ACCEPTED);
        }
        
        return true;
    }

    public boolean completeContract(Player player, ActiveContract ac) {
        ContractDefinition def = contractConfig.getContract(ac.getContractId());
        if (def == null) return false;

        double reward = def.rewardMoney();
        boolean boosted = false;
        if (eventModifierService != null) {
            double multiplier = eventModifierService.getRewardMultiplier(def.category());
            reward *= multiplier;
            boosted = multiplier > 1.0;
        }

        // Payout
        economy.depositPlayer(player, reward);
        
        persistence.getPlayerContracts(player.getUniqueId()).remove(ac);
        persistence.save();

        if (metricsManager != null) {
            metricsManager.increment(CityMetricKeys.CONTRACTS_COMPLETED);
            metricsManager.increment(CityMetricKeys.CONTRACTS_PAYOUTS_TOTAL, reward);
            if (boosted) {
                metricsManager.increment(CityMetricKeys.CONTRACTS_EVENT_BOOSTED_COMPLETIONS);
            }
        }
        
        return true;
    }

    public void abandonContract(Player player, ActiveContract ac) {
        ContractDefinition def = contractConfig.getContract(ac.getContractId());
        if (def == null) return;

        // Set cooldown
        Instant cooldownUntil = Instant.now().plus(Duration.ofMinutes(def.cooldownMinutes()));
        persistence.getPlayerCooldowns(player.getUniqueId()).put(ac.getContractId(), cooldownUntil);
        
        persistence.getPlayerContracts(player.getUniqueId()).remove(ac);
        persistence.save();

        if (metricsManager != null) {
            metricsManager.increment(CityMetricKeys.CONTRACTS_ABANDONED);
        }
    }

    public void failContract(Player player, ActiveContract ac) {
        abandonContract(player, ac); // Same logic for V1: no payout, apply cooldown
        if (metricsManager != null) {
            metricsManager.increment(CityMetricKeys.CONTRACTS_FAILED);
        }
    }

    public void cleanupExpiredContracts() {
        // Logic to run on a task to fail expired contracts
    }

    public ContractConfigManager getContractConfig() { return contractConfig; }
    public EndpointConfigManager getEndpointConfig() { return endpointConfig; }
    public SettingsConfigManager getSettingsConfig() { return settingsConfig; }
    public ContractPersistenceService getPersistence() { return persistence; }
}
