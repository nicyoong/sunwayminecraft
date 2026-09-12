package com.sunwayMinecraft.contracts.persistence;

import com.sunwayMinecraft.contracts.domain.ActiveContract;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * In-memory facade over the SQLite contract store. Callers mutate the
 * returned live lists/maps (accept, complete, abandon) and then call
 * {@link #save()}, which rewrites the active-contract and cooldown tables.
 * Immediate repository operations (progress state, expiries, stats) go
 * through the {@link ContractDatabase} accessors.
 */
public class ContractPersistenceService {
    private final ContractDatabase database;
    private final Map<UUID, List<ActiveContract>> activeContracts = new HashMap<>();
    private final Map<UUID, Map<String, Instant>> cooldowns = new HashMap<>();

    public ContractPersistenceService(JavaPlugin plugin) {
        this.database = new ContractDatabase(plugin);
        load();
    }

    public void load() {
        activeContracts.clear();
        for (ActiveContract contract : database.getActiveContracts()) {
            activeContracts.computeIfAbsent(contract.getPlayerUuid(), k -> new ArrayList<>())
                    .add(contract);
        }
        cooldowns.clear();
        cooldowns.putAll(database.loadCooldowns());
    }

    public void save() {
        for (List<ActiveContract> playerContracts : activeContracts.values()) {
            for (ActiveContract contract : playerContracts) {
                database.addActiveContract(contract);
            }
        }
        // rows still active in SQLite but gone from the live lists must disappear
        for (Map.Entry<UUID, List<ActiveContract>> entry : staleEntries().entrySet()) {
            for (ActiveContract ghost : entry.getValue()) {
                database.removeActiveContract(ghost.getPlayerUuid(), ghost.getContractId());
            }
        }
        database.saveCooldowns(cooldowns);
    }

    /** Rows still marked active in SQLite but gone from the in-memory lists. */
    private Map<UUID, List<ActiveContract>> staleEntries() {
        Map<UUID, List<ActiveContract>> stale = new HashMap<>();
        for (ActiveContract stored : database.getActiveContracts()) {
            List<ActiveContract> live = activeContracts.get(stored.getPlayerUuid());
            boolean present = live != null && live.stream()
                    .anyMatch(contract -> contract.getContractId().equals(stored.getContractId()));
            if (!present) {
                stale.computeIfAbsent(stored.getPlayerUuid(), k -> new ArrayList<>()).add(stored);
            }
        }
        return stale;
    }

    public List<ActiveContract> getPlayerContracts(UUID uuid) {
        return activeContracts.computeIfAbsent(uuid, k -> new ArrayList<>());
    }

    public Map<String, Instant> getPlayerCooldowns(UUID uuid) {
        return cooldowns.computeIfAbsent(uuid, k -> new HashMap<>());
    }

    public boolean hasActiveContract(UUID uuid, String contractId) {
        return getPlayerContracts(uuid).stream()
                .anyMatch(contract -> contract.getContractId().equals(contractId));
    }

    /** Persists one contract's progress stages without rewriting the whole store. */
    public void updateProgressState(ActiveContract contract) {
        database.updateProgressState(contract.getPlayerUuid(), contract.getContractId(),
                contract.getProgressState());
    }

    /** Flips overdue rows to expired in SQLite and drops them from memory. */
    public int expireContracts() {
        int expired = database.expireContracts(System.currentTimeMillis());
        for (List<ActiveContract> playerContracts : activeContracts.values()) {
            playerContracts.removeIf(ActiveContract::isExpired);
        }
        return expired;
    }

    public void recordCompletion(UUID playerUuid, String contractId, String alignmentId,
                                 String campusId, Instant completedAt,
                                 double rewardAmount, long reputationAwarded) {
        database.recordCompletion(playerUuid, contractId, alignmentId, campusId, completedAt,
                rewardAmount, reputationAwarded);
    }

    public ContractDatabase getDatabase() { return database; }

    public void close() { database.close(); }

    /** Exposes active contracts for cleanup and reporting; callers may mutate the contained lists. */
    public Map<UUID, List<ActiveContract>> getAllPlayerContracts() {
        return activeContracts;
    }
}
