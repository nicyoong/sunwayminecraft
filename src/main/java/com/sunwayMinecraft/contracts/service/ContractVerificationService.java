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
        ContractDefinition def = manager.getContractConfig().getContract(ac.getContractId());
        if (def == null) return new VerificationResult(false, "Contract definition not found.");

        if (ac.isExpired()) {
            manager.failContract(player, ac);
            return new VerificationResult(false, "Contract has expired.");
        }

        ContractEndpoint endPoint = manager.getEndpointConfig().getEndpoint(def.endEndpointId());
        if (endPoint == null) return new VerificationResult(false, "Destination endpoint not found.");

        // Distance check
        if (!player.getWorld().equals(endPoint.location().getWorld()) || 
            player.getLocation().distance(endPoint.location()) > endPoint.radius()) {
            return new VerificationResult(false, "You must be at " + endPoint.name() + " to complete this.");
        }

        switch (def.category()) {
            case HAULING -> {
                return verifyHauling(player, def);
            }
            case COURIER, SURVEY, MAINTENANCE, RECOVERY -> {
                // For V1, these are primarily location-based or simple interact-based
                // Maintenance might need a prior interaction flag, but for V1 we'll allow completion if at the spot.
                return new VerificationResult(true, "Objective met.");
            }
            default -> {
                return new VerificationResult(false, "Unknown contract category.");
            }
        }
    }

    private VerificationResult verifyHauling(Player player, ContractDefinition def) {
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

        return new VerificationResult(true, "Items delivered.");
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
