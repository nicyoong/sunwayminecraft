package com.sunwayMinecraft.contracts.service;

import com.sunwayMinecraft.contracts.domain.ActiveContract;
import com.sunwayMinecraft.contracts.domain.ContractDefinition;
import com.sunwayMinecraft.contracts.domain.ContractEndpoint;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class ContractVerificationService {
    private final ContractsManager manager;

    public ContractVerificationService(ContractsManager manager) {
        this.manager = manager;
    }

    public VerificationResult verifyCompletion(Player player, ActiveContract ac) {
        if (ac == null || !player.getUniqueId().equals(ac.getPlayerUuid())
                || !manager.getPersistence().getPlayerContracts(player.getUniqueId()).contains(ac)) {
            return new VerificationResult(false, "This contract is not active for you.");
        }

        ContractDefinition def = manager.getContractConfig().getContract(ac.getContractId());
        if (def == null) return new VerificationResult(false, "Contract definition not found.");

        if (ac.isExpired()) {
            manager.failContract(player, ac);
            return new VerificationResult(false, "Contract has expired.");
        }

        ContractEndpoint endPoint = manager.getEndpointConfig().getEndpoint(def.endEndpointId());
        if (endPoint == null) return new VerificationResult(false, "Destination endpoint not found.");

        // Distance check
        if (!ContractObjectiveService.isWithinEndpoint(player.getLocation(), endPoint)) {
            return new VerificationResult(false, "You must be at " + endPoint.name() + " to complete this.");
        }

        return switch (def.objectiveType()) {
            case DELIVER_MATERIALS -> verifyHauling(player, def, ac);
            case REACH_DESTINATION -> completeObjective(ac, "Destination reached.");
            case INTERACT_AT_DESTINATION -> ac.isObjectiveComplete()
                    ? new VerificationResult(true, "Objective met.")
                    : new VerificationResult(false, "Interact with the target before completing this contract.");
        };
    }

    private VerificationResult verifyHauling(Player player, ContractDefinition def, ActiveContract active) {
        Map<Material, Integer> required = def.requiredMaterials();
        for (Map.Entry<Material, Integer> entry : required.entrySet()) {
            if (!player.getInventory().containsAtLeast(new ItemStack(entry.getKey()), entry.getValue())) {
                return new VerificationResult(false, "Missing " + entry.getValue() + " " + entry.getKey().name());
            }
        }

        // Consume items
        for (Map.Entry<Material, Integer> entry : required.entrySet()) {
            removeFromInventory(player, entry.getKey(), entry.getValue());
        }

        return completeObjective(active, "Items delivered.");
    }

    private VerificationResult completeObjective(ActiveContract active, String message) {
        active.completeObjective();
        manager.getPersistence().save();
        return new VerificationResult(true, message);
    }

    private void removeFromInventory(Player player, Material material, int amount) {
        ItemStack[] contents = player.getInventory().getContents();
        int remaining = amount;
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == material) {
                if (item.getAmount() <= remaining) {
                    remaining -= item.getAmount();
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(item.getAmount() - remaining);
                    remaining = 0;
                }
            }
            if (remaining <= 0) break;
        }
    }

    public record VerificationResult(boolean success, String message) {}
}
