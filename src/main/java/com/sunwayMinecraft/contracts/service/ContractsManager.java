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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public class ContractsManager {
    private final JavaPlugin plugin;
    private final ContractConfigManager contractConfig;
    private final EndpointConfigManager endpointConfig;
    private final SettingsConfigManager settingsConfig;
    private final ContractPersistenceService persistence;
    private final Economy economy;
    private EventModifierService eventModifierService;
    private CityMetricsManager metricsManager;
    private ContractDiplomacyService diplomacyService;
    private ContractSupplyService supplyService;
    private Function<UUID, Optional<String>> alignmentLookup = uuid -> Optional.empty();
    private boolean warnedNoEconomy = false;
    private java.util.function.ObjIntConsumer<UUID> reputationRewarder = (uuid, amount) -> { };

    /** Applies a signed reputation delta to the player's alignment membership. */
    public void setReputationRewarder(java.util.function.ObjIntConsumer<UUID> reputationRewarder) {
        this.reputationRewarder = reputationRewarder != null
                ? reputationRewarder : (uuid, amount) -> { };
    }

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

    /** Optional strategic hooks; null-safe when the diplomacy/supply layer is absent. */
    public void setDiplomacyService(ContractDiplomacyService diplomacyService) {
        this.diplomacyService = diplomacyService;
    }

    public void setSupplyService(ContractSupplyService supplyService) {
        this.supplyService = supplyService;
    }

    /** Late-bound player alignment resolver; unaligned players resolve to empty. */
    public void setAlignmentLookup(Function<UUID, Optional<String>> alignmentLookup) {
        this.alignmentLookup = alignmentLookup != null ? alignmentLookup : uuid -> Optional.empty();
    }

    /** True when the alignment satisfies the contract's required/forbidden rule. */
    public boolean canAlignmentAcceptContract(String alignmentId, ContractDefinition contract) {
        return contract != null && contract.alignmentRule().canAccept(alignmentId);
    }

    /** The player's current alignment id, or null when unaligned or unresolvable. */
    public String getAlignmentFor(Player player) {
        return alignmentLookup.apply(player.getUniqueId()).orElse(null);
    }

    /** Enabled contracts the given alignment may accept. */
    public List<ContractDefinition> getContractsForAlignment(String alignmentId) {
        return contractConfig.getContracts().values().stream()
                .filter(def -> def.alignmentRule().canAccept(alignmentId))
                .toList();
    }

    /** Enabled contracts whose route starts at, ends at or touches the campus. */
    public List<ContractDefinition> getContractsByCampus(String campusId) {
        return contractConfig.getContracts().values().stream()
                .filter(def -> def.campusRoute().touchesCampus(campusId))
                .toList();
    }

    /** Enabled contracts running from the origin campus to the destination campus. */
    public List<ContractDefinition> getContractsBetweenCampuses(String originCampus,
                                                                String destinationCampus) {
        return contractConfig.getContracts().values().stream()
                .filter(def -> def.campusRoute().isBetween(originCampus, destinationCampus))
                .toList();
    }

    /**
     * Disables contracts whose endpoint references are broken: unknown start or
     * end ids always, and delivery contracts not ending at a dropoff or depot.
     */
    public void validateEndpointReferences() {
        for (String id : List.copyOf(contractConfig.getContracts().keySet())) {
            ContractDefinition def = contractConfig.getContract(id);
            String problem = endpointProblem(def);
            if (problem != null) {
                contractConfig.disableContract(id, problem);
            }
        }
    }

    private String endpointProblem(ContractDefinition def) {
        if (def.startEndpointId() == null
                || endpointConfig.getEndpoint(def.startEndpointId()) == null) {
            return "unknown start endpoint: " + def.startEndpointId();
        }
        if (def.endEndpointId() == null
                || endpointConfig.getEndpoint(def.endEndpointId()) == null) {
            return "unknown end endpoint: " + def.endEndpointId();
        }
        if (def.category() == ContractCategory.DELIVERY) {
            var type = endpointConfig.getEndpoint(def.endEndpointId()).type();
            if (type != ContractEndpoint.EndpointType.DROPOFF
                    && type != ContractEndpoint.EndpointType.DEPOT) {
                return "delivery contracts must end at a dropoff or depot endpoint";
            }
        }
        return null;
    }

    public boolean acceptContract(Player player, String contractId) {
        if (acceptanceProblem(player, contractId) != null) return false;
        ContractDefinition def = contractConfig.getContract(contractId);
        UUID uuid = player.getUniqueId();
        Instant expiry = Instant.now().plus(Duration.ofMinutes(def.durationMinutes()));
        ActiveContract newContract = new ActiveContract(uuid, contractId, Instant.now(), expiry);
        persistence.getPlayerContracts(uuid).add(newContract);
        persistence.save();

        if (metricsManager != null) {
            metricsManager.increment(CityMetricKeys.CONTRACTS_ACCEPTED);
        }
        return true;
    }

    /**
     * The reason a player cannot accept the contract, or null when they can.
     * Covers enabled-ness (unknown id), the active limit, the same-contract
     * duplicate rule, cooldowns and the alignment requirement.
     */
    public String acceptanceProblem(Player player, String contractId) {
        ContractDefinition def = contractConfig.getContract(contractId);
        if (def == null || !def.enabled()) return "Contract unavailable.";

        UUID uuid = player.getUniqueId();
        List<ActiveContract> active = persistence.getPlayerContracts(uuid);

        if (active.size() >= settingsConfig.getMaxActiveContracts()) {
            return "You already have the maximum number of active contracts.";
        }
        if (!settingsConfig.isAllowMultipleSameContract()
                && active.stream().anyMatch(c -> c.getContractId().equals(contractId))) {
            return "You already have this contract active.";
        }

        Instant cooldownUntil = persistence.getPlayerCooldowns(uuid).get(contractId);
        if (cooldownUntil != null && Instant.now().isBefore(cooldownUntil)) {
            return "This contract is on cooldown.";
        }

        if (!def.alignmentRule().canAccept(alignmentLookup.apply(uuid).orElse(null))) {
            return "Your alignment cannot accept this contract.";
        }
        return null;
    }

    public boolean completeContract(Player player, ActiveContract ac) {
        if (ac == null || !player.getUniqueId().equals(ac.getPlayerUuid())
                || ac.isExpired() || !ac.isObjectiveComplete()
                || !persistence.getPlayerContracts(player.getUniqueId()).contains(ac)) {
            return false;
        }

        ContractDefinition def = contractConfig.getContract(ac.getContractId());
        if (def == null) return false;

        // Idempotency: remove from the live list before paying so a repeated
        // command cannot double-award the same completion.
        persistence.getPlayerContracts(player.getUniqueId()).remove(ac);

        String alignmentId = alignmentLookup.apply(player.getUniqueId()).orElse(null);
        Reward reward = computeReward(def, alignmentId);
        if (ac.hasStage("sabotaged")) {
            // a successful sabotage halves the completion reward
            reward = new Reward(reward.money() * 0.5, reward.reputation(), reward.boosted());
        }

        double money = reward.money();
        if (money > 0) {
            if (economy != null) {
                economy.depositPlayer(player, money);
            } else if (!warnedNoEconomy) {
                warnedNoEconomy = true;
                logWarning("Vault economy unavailable; contract money rewards are disabled");
            }
        }

        if (reward.reputation() > 0 && alignmentId != null) {
            reputationRewarder.accept(player.getUniqueId(), (int) reward.reputation());
        }

        String campus = def.campusRoute().homeCampusOrOrigin();
        Instant now = Instant.now();
        persistence.recordCompletion(player.getUniqueId(), def.id(), alignmentId, campus,
                now, money, reward.reputation());
        persistence.save();

        if (diplomacyService != null) {
            diplomacyService.recordCompletion(def, player.getUniqueId(), alignmentId);
        }
        if (supplyService != null) {
            supplyService.recordCompletion(def, alignmentId);
        }

        if (metricsManager != null) {
            metricsManager.increment(CityMetricKeys.CONTRACTS_COMPLETED);
            metricsManager.increment(CityMetricKeys.CONTRACTS_PAYOUTS_TOTAL, money);
            if (reward.boosted()) {
                metricsManager.increment(CityMetricKeys.CONTRACTS_EVENT_BOOSTED_COMPLETIONS);
            }
            if (alignmentId != null) {
                metricsManager.increment(CityMetricKeys.CONTRACTS_COMPLETED + "." + alignmentId);
            }
        }
        if (pluginLog() != null) {
            pluginLog().info("Contract '" + def.id() + "' completed by " + player.getName()
                    + ": $" + money + ", " + reward.reputation() + " rep"
                    + (reward.boosted() ? " (boosted)" : ""));
        }
        return true;
    }

    private void logWarning(String message) {
        if (pluginLog() != null) pluginLog().warning(message);
    }

    private java.util.logging.Logger pluginLog() {
        return plugin == null ? null : plugin.getLogger();
    }

    /**
     * Money and reputation for a completion: base contract reward, plus the
     * recommended-alignment and cross-campus bonuses, with the active city-event
     * multiplier applied to the money total.
     */
    Reward computeReward(ContractDefinition def, String alignmentId) {
        double money = def.rewardMoney();
        long reputation = def.rewardReputation();

        ContractAlignmentRule rule = def.alignmentRule();
        if (alignmentId != null && rule.recommendedAlignment() != null
                && alignmentId.equalsIgnoreCase(rule.recommendedAlignment())) {
            money += settingsConfig.getRecommendedBonusMoney();
            reputation += settingsConfig.getRecommendedBonusReputation();
        }
        String origin = def.campusRoute().originCampus();
        String destination = def.campusRoute().destinationCampus();
        if (origin != null && destination != null && !origin.equalsIgnoreCase(destination)) {
            money += settingsConfig.getCrossCampusBonusMoney();
            reputation += settingsConfig.getCrossCampusBonusReputation();
        }

        boolean boosted = false;
        if (eventModifierService != null) {
            double multiplier = eventModifierService.getRewardMultiplier(def.category());
            money *= multiplier;
            boosted = multiplier > 1.0;
        }
        return new Reward(money, reputation, boosted);
    }

    public record Reward(double money, long reputation, boolean boosted) {}

    /** Estimated money/reputation a viewing alignment would receive, for board listings. */
    public Reward estimateReward(String alignmentId, ContractDefinition def) {
        return computeReward(def, alignmentId);
    }

    public void abandonContract(Player player, ActiveContract ac) {
        ContractDefinition def = contractConfig.getContract(ac.getContractId());
        if (def == null) return;

        // Cooldown: settings seconds override when configured, else per-contract minutes
        long cooldownSeconds = settingsConfig.getAbandonmentCooldownSeconds();
        Instant cooldownUntil = cooldownSeconds > 0
                ? Instant.now().plusSeconds(cooldownSeconds)
                : Instant.now().plus(Duration.ofMinutes(def.cooldownMinutes()));
        persistence.getPlayerCooldowns(player.getUniqueId()).put(ac.getContractId(), cooldownUntil);

        persistence.getPlayerContracts(player.getUniqueId()).remove(ac);
        persistence.save();

        applyAbandonmentPenalty(player, def);

        if (pluginLog() != null) {
            pluginLog().info("Contract '" + def.id() + "' abandoned by " + player.getName());
        }
        if (metricsManager != null) {
            metricsManager.increment(CityMetricKeys.CONTRACTS_ABANDONED);
        }
    }

    /** Charges the configured reputation penalty, doubled for diplomatic/emergency. */
    private void applyAbandonmentPenalty(Player player, ContractDefinition def) {
        int penalty = isHighStakes(def)
                ? settingsConfig.getDiplomaticEmergencyReputationPenalty()
                : settingsConfig.getAbandonmentReputationPenalty();
        if (penalty > 0) {
            reputationRewarder.accept(player.getUniqueId(), -penalty);
        }
    }

    private boolean isHighStakes(ContractDefinition def) {
        return def.category() == ContractCategory.DIPLOMATIC
                || def.category() == ContractCategory.EMERGENCY;
    }

    public void failContract(Player player, ActiveContract ac) {
        abandonContract(player, ac); // Same logic for V1: no payout, apply cooldown
        if (metricsManager != null) {
            metricsManager.increment(CityMetricKeys.CONTRACTS_FAILED);
        }
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
    public java.util.logging.Logger getLogger() { return plugin.getLogger(); }
}
