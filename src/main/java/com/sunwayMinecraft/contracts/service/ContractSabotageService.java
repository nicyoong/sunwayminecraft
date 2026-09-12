package com.sunwayMinecraft.contracts.service;

import com.sunwayMinecraft.contracts.config.ContractDiplomacySettings;
import com.sunwayMinecraft.contracts.config.ContractConfigManager;
import com.sunwayMinecraft.contracts.domain.ActiveContract;
import com.sunwayMinecraft.contracts.domain.ContractDefinition;
import com.sunwayMinecraft.contracts.persistence.ContractDatabase;
import com.sunwayMinecraft.contracts.persistence.ContractPersistenceService;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Sabotage of other players' active contracts (never world terrain). Enforces
 * enablement, per-player cooldown, no self/friendly sabotage, no completed or
 * expired targets and a Vault cost; a successful roll delays the victim's
 * progress and can siphon influence, a failure charges reputation. Attempts are
 * logged. An admin bypass supports testing.
 */
public class ContractSabotageService {
    /** The stage that halves a sabotaged contract's completion reward. */
    public static final String STAGE_SABOTAGED = "sabotaged";

    private final ContractDatabase database;
    private final ContractPersistenceService persistence;
    private final ContractConfigManager contractConfig;
    private final ContractDiplomacySettings settings;
    private final ContractDiplomacyService diplomacy;
    private final Economy economy;
    private final Function<UUID, Optional<String>> alignmentLookup;
    private final java.util.function.ObjIntConsumer<UUID> reputationRewarder;
    private final org.bukkit.plugin.java.JavaPlugin plugin;
    private final Map<UUID, Instant> cooldowns = new HashMap<>();
    private final java.util.Random random;

    public ContractSabotageService(ContractDatabase database, ContractPersistenceService persistence,
                                   ContractConfigManager contractConfig, ContractDiplomacySettings settings,
                                   ContractDiplomacyService diplomacy, Economy economy,
                                   Function<UUID, Optional<String>> alignmentLookup,
                                   java.util.function.ObjIntConsumer<UUID> reputationRewarder,
                                   org.bukkit.plugin.java.JavaPlugin plugin) {
        this(database, persistence, contractConfig, settings, diplomacy, economy, alignmentLookup,
                reputationRewarder, plugin, new java.util.Random());
    }

    public ContractSabotageService(ContractDatabase database, ContractPersistenceService persistence,
                                   ContractConfigManager contractConfig, ContractDiplomacySettings settings,
                                   ContractDiplomacyService diplomacy, Economy economy,
                                   Function<UUID, Optional<String>> alignmentLookup,
                                   java.util.function.ObjIntConsumer<UUID> reputationRewarder,
                                   org.bukkit.plugin.java.JavaPlugin plugin, java.util.Random random) {
        this.database = database;
        this.persistence = persistence;
        this.contractConfig = contractConfig;
        this.settings = settings;
        this.diplomacy = diplomacy;
        this.economy = economy;
        this.alignmentLookup = alignmentLookup;
        this.reputationRewarder = reputationRewarder;
        this.plugin = plugin;
        this.random = random;
    }

    public record SabotageResult(boolean attempted, boolean success, String message) {
        static SabotageResult blocked(String why) { return new SabotageResult(false, false, why); }
    }

    public SabotageResult attempt(Player saboteur, int activeId) {
        return attempt(saboteur, activeId, false);
    }

    /** @param adminBypass skips enablement/cooldown checks and forces success. */
    public SabotageResult attempt(Player saboteur, int activeId, boolean adminBypass) {
        if (!settings.isSabotageEnabled() && !adminBypass) {
            return SabotageResult.blocked("Sabotage is not enabled on this server.");
        }
        Instant last = cooldowns.get(saboteur.getUniqueId());
        if (!adminBypass && last != null
                && Instant.now().isBefore(last.plusSeconds(settings.getSabotageCooldownSeconds()))) {
            long remaining = last.plusSeconds(settings.getSabotageCooldownSeconds())
                    .getEpochSecond() - Instant.now().getEpochSecond();
            return SabotageResult.blocked("Sabotage on cooldown for " + remaining + "s.");
        }

        ActiveContract target = database.getActiveContractById(activeId);
        if (target == null) return SabotageResult.blocked("No active contract with that id.");
        if (target.getPlayerUuid().equals(saboteur.getUniqueId())) {
            return SabotageResult.blocked("You cannot sabotage your own contract.");
        }
        if (target.isObjectiveComplete()) {
            return SabotageResult.blocked("That contract objective is already complete.");
        }
        ContractDefinition def = contractConfig.getContract(target.getContractId());
        if (def == null) return SabotageResult.blocked("That contract no longer exists.");

        String saboteurAlignment = alignmentLookup.apply(saboteur.getUniqueId()).orElse(null);
        String victimAlignment = alignmentLookup.apply(target.getPlayerUuid()).orElse(null);
        if (saboteurAlignment != null && saboteurAlignment.equalsIgnoreCase(victimAlignment)) {
            return SabotageResult.blocked("You cannot sabotage an ally's contract.");
        }

        // Charge the (money) cost up front; skipped gracefully without Vault.
        if (settings.getSabotageItemCost() > 0 && economy != null) {
            economy.withdrawPlayer(saboteur, settings.getSabotageItemCost());
        }
        cooldowns.put(saboteur.getUniqueId(), Instant.now());

        boolean success = adminBypass
                || random.nextDouble() < settings.getSabotageSuccessChance();
        logAttempt(saboteur, activeId, target, success);

        if (success) {
            applySuccess(saboteur, activeId, target, victimAlignment);
        } else {
            applyFailure(saboteur, activeId);
        }
        return new SabotageResult(true, success, success
                ? "You sabotage the contract - their progress is set back."
                : "Your sabotage fails; you suffer a reputation hit.");
    }

    private void applySuccess(Player saboteur, int activeId, ActiveContract target,
                              String victimAlignment) {
        database.delayActiveContract(activeId, settings.getSabotageCooldownSeconds());
        for (ActiveContract live : persistence.getPlayerContracts(target.getPlayerUuid())) {
            if (live.getActiveId() == activeId) {
                live.setProgress(0.0);
                live.markStage(STAGE_SABOTAGED);
                persistence.updateProgressState(live);
                persistence.save();
            }
        }
        if (victimAlignment != null) {
            diplomacy.adminAdjustInfluence(victimAlignment,
                    -Math.max(1, settings.getInfluencePerCompletion()));
        }
        notifyVictim(target.getPlayerUuid(), saboteur.getName());
    }

    private void applyFailure(Player saboteur, int activeId) {
        int penalty = settings.getSabotageFailurePenalty();
        if (penalty > 0) {
            reputationRewarder.accept(saboteur.getUniqueId(), -penalty);
        }
    }

    private void notifyVictim(UUID victim, String attackerName) {
        Player online = Bukkit.getPlayer(victim);
        if (online != null) {
            online.sendMessage("§cSomeone sabotaged one of your active contracts!");
        }
    }

    private void logAttempt(Player saboteur, int activeId, ActiveContract target, boolean success) {
        if (plugin != null && plugin.getLogger() != null) {
            plugin.getLogger().info("Sabotage: " + saboteur.getName() + " on contract#"
                    + activeId + " (" + target.getContractId() + " of " + target.getPlayerUuid()
                    + ") -> " + (success ? "SUCCESS" : "FAIL"));
        }
    }

    /** Admin/testing: clear a player's sabotage cooldown. */
    public void resetCooldown(UUID player) {
        cooldowns.remove(player);
    }

    /** Every active contract a player currently holds, for the sabotage picker. */
    public List<ActiveContract> activeFor(UUID playerUuid) {
        return persistence.getPlayerContracts(playerUuid);
    }
}
