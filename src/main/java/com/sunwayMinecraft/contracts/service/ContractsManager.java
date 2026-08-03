package com.sunwayMinecraft.contracts.service;

import com.sunwayMinecraft.contracts.config.ContractConfigManager;
import com.sunwayMinecraft.contracts.config.EndpointConfigManager;
import com.sunwayMinecraft.contracts.config.SettingsConfigManager;
import com.sunwayMinecraft.contracts.domain.*;
import com.sunwayMinecraft.contracts.persistence.ContractPersistenceService;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ContractsManager {
    private final JavaPlugin plugin;
    private final ContractConfigManager contractConfig;
    private final EndpointConfigManager endpointConfig;
    private final SettingsConfigManager settingsConfig;
    private final ContractPersistenceService persistence;
    private final Economy economy;

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

    public boolean acceptContract(Player player, String contractId) {
        ContractDefinition def = contractConfig.getContract(contractId);
        if (def == null) return false;

        UUID uuid = player.getUniqueId();
        List<ActiveContract> active = persistence.getPlayerContracts(uuid);
        
        if (active.size() >= settingsConfig.getMaxActiveContracts()) return false;
        if (active.stream().anyMatch(contract -> contract.getContractId().equals(contractId))) return false;
        
        // Check cooldown
        Instant cooldownUntil = persistence.getPlayerCooldowns(uuid).get(contractId);
        if (cooldownUntil != null && Instant.now().isBefore(cooldownUntil)) return false;

        Instant expiry = Instant.now().plus(Duration.ofMinutes(def.durationMinutes()));
        ActiveContract newContract = new ActiveContract(uuid, contractId, Instant.now(), expiry);
        active.add(newContract);
        persistence.save();
        return true;
    }

    public boolean completeContract(Player player, ActiveContract ac) {
        if (ac == null || !player.getUniqueId().equals(ac.getPlayerUuid())
                || ac.isExpired() || !ac.isObjectiveComplete()
                || !persistence.getPlayerContracts(player.getUniqueId()).contains(ac)) {
            return false;
        }

        ContractDefinition def = contractConfig.getContract(ac.getContractId());
        if (def == null || economy == null) return false;

        // Payout
        economy.depositPlayer(player, def.rewardMoney());
        
        persistence.getPlayerContracts(player.getUniqueId()).remove(ac);
        persistence.save();
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
    }

    public void failContract(Player player, ActiveContract ac) {
        abandonContract(player, ac); // Same logic for V1: no payout, apply cooldown
    }

    public void cleanupExpiredContracts() {
        boolean changed = false;
        for (Map.Entry<UUID, List<ActiveContract>> entry : persistence.getAllPlayerContracts().entrySet()) {
            List<ActiveContract> active = entry.getValue();
            for (java.util.Iterator<ActiveContract> iterator = active.iterator(); iterator.hasNext();) {
                ActiveContract contract = iterator.next();
                if (!contract.isExpired()) continue;

                ContractDefinition definition = contractConfig.getContract(contract.getContractId());
                if (definition != null) {
                    persistence.getPlayerCooldowns(entry.getKey()).put(contract.getContractId(),
                            Instant.now().plus(Duration.ofMinutes(definition.cooldownMinutes())));
                }
                iterator.remove();
                changed = true;
            }
        }
        if (changed) persistence.save();
    }

    public ContractConfigManager getContractConfig() { return contractConfig; }
    public EndpointConfigManager getEndpointConfig() { return endpointConfig; }
    public SettingsConfigManager getSettingsConfig() { return settingsConfig; }
    public ContractPersistenceService getPersistence() { return persistence; }
}
