package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.contracts.config.ContractDiplomacySettings;
import com.sunwayMinecraft.contracts.domain.ActiveContract;
import com.sunwayMinecraft.contracts.domain.InfluenceRecord;
import com.sunwayMinecraft.contracts.service.ContractsManager;
import com.sunwayMinecraft.contracts.service.ContractDiplomacyService;
import com.sunwayMinecraft.contracts.service.ContractSabotageService;
import com.sunwayMinecraft.contracts.service.ContractSupplyService;
import com.sunwayMinecraft.contracts.service.DynamicContractService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * Chat/command surface for the strategic layer: sabotage, influence, diplomacy
 * activity and supply, plus the admin strategic tools. Every collaborator is
 * optional so the command degrades to a clear "not enabled" notice when a
 * subsystem is absent.
 */
public class ContractsStrategicCommands {
    private static final int PAGE_SIZE = 6;
    private static final String ADMIN = "sunway.contracts.admin";
    private static final String SABOTAGE = "sunway.contracts.sabotage";

    private final ContractsManager manager;
    private ContractDiplomacyService diplomacy;
    private ContractSupplyService supply;
    private ContractSabotageService sabotage;
    private DynamicContractService dynamic;
    private ContractDiplomacySettings diplomacySettings;

    public ContractsStrategicCommands(ContractsManager manager) {
        this.manager = manager;
    }

    public void setServices(ContractDiplomacyService diplomacy, ContractSupplyService supply,
                           ContractSabotageService sabotage, DynamicContractService dynamic,
                           ContractDiplomacySettings diplomacySettings) {
        this.diplomacy = diplomacy;
        this.supply = supply;
        this.sabotage = sabotage;
        this.dynamic = dynamic;
        this.diplomacySettings = diplomacySettings;
    }

    // ---- player commands -------------------------------------------------

    /** /contracts sabotage <active_id> */
    public void sabotage(Player player, String[] args) {
        if (!player.hasPermission(SABOTAGE)) {
            player.sendMessage(Component.text("You do not have permission to sabotage.", NamedTextColor.RED));
            return;
        }
        if (sabotage == null) { player.sendMessage(unavailable()); return; }
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /contracts sabotage <active_id>", NamedTextColor.RED));
            player.sendMessage(Component.text("Your active contract ids:", NamedTextColor.GRAY));
            for (ActiveContract ac : manager.getPersistence().getPlayerContracts(player.getUniqueId())) {
                player.sendMessage(Component.text("  #" + ac.getActiveId() + " " + ac.getContractId(),
                        NamedTextColor.GRAY));
            }
            return;
        }
        int activeId;
        try {
            activeId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Active id must be a number.", NamedTextColor.RED));
            return;
        }
        ContractSabotageService.SabotageResult result = sabotage.attempt(player, activeId);
        NamedTextColor color = !result.attempted() ? NamedTextColor.RED
                : result.success() ? NamedTextColor.GREEN : NamedTextColor.YELLOW;
        player.sendMessage(Component.text(result.message(), color));
    }

    /** /contracts influence [alignment] */
    public void influence(CommandSender sender, String[] args) {
        if (diplomacy == null) { sender.sendMessage(unavailable()); return; }
        String target;
        if (args.length >= 2) {
            if (!sender.hasPermission(ADMIN)) { sender.sendMessage(denied()); return; }
            target = args[1].toLowerCase();
        } else if (sender instanceof Player player) {
            target = manager.getAlignmentFor(player);
        } else {
            sender.sendMessage(Component.text("Only players can use /contracts influence without an argument.",
                    NamedTextColor.RED));
            return;
        }
        if (target == null) {
            sender.sendMessage(Component.text("You have no alignment.", NamedTextColor.GRAY));
            return;
        }
        sender.sendMessage(Component.text("Influence for " + target + ": ", NamedTextColor.GOLD)
                .append(Component.text(String.valueOf(diplomacy.getInfluence(target)), NamedTextColor.AQUA)));
    }

    /** /contracts diplomacy [between <a> <b>] [page] */
    public void diplomacy(CommandSender sender, String[] args) {
        if (diplomacy == null) { sender.sendMessage(unavailable()); return; }
        if (args.length >= 4 && args[1].equalsIgnoreCase("between")) {
            if (!sender.hasPermission(ADMIN)) { sender.sendMessage(denied()); return; }
            int total = diplomacy.getInfluenceBetween(args[2].toLowerCase(), args[3].toLowerCase());
            sender.sendMessage(Component.text("Influence between " + args[2] + " and " + args[3] + ": ",
                    NamedTextColor.GOLD).append(Component.text(String.valueOf(total), NamedTextColor.AQUA)));
            return;
        }
        int page = parsePage(args.length >= 2 ? args[args.length - 1] : "1");
        List<InfluenceRecord> recent = diplomacy.getRecentInfluence(page * PAGE_SIZE);
        sender.sendMessage(Component.text("=== Recent Influence (page " + page + ") ===",
                NamedTextColor.GOLD));
        if (recent.isEmpty()) {
            sender.sendMessage(Component.text("No influence activity yet.", NamedTextColor.GRAY));
            return;
        }
        int start = (page - 1) * PAGE_SIZE;
        for (int i = start; i < Math.min(start + PAGE_SIZE, recent.size()); i++) {
            InfluenceRecord r = recent.get(i);
            sender.sendMessage(Component.text("- " + r.influenceType() + " " + r.influenceAmount()
                    + " on " + r.contractId() + (r.originAlignmentId() != null
                    ? " (" + r.originAlignmentId() + ")" : ""), NamedTextColor.GRAY));
        }
    }

    /** /contracts supply [alliance <id>] */
    public void supply(CommandSender sender, String[] args) {
        if (supply == null) { sender.sendMessage(unavailable()); return; }
        if (args.length >= 3 && args[1].equalsIgnoreCase("alliance")) {
            if (!sender.hasPermission(ADMIN)) { sender.sendMessage(denied()); return; }
            sender.sendMessage(Component.text("Supply for alliance " + args[2] + ": ", NamedTextColor.GOLD)
                    .append(Component.text(
                            String.valueOf(supply.getSupplyPointsForAlliance(args[2].toLowerCase())),
                            NamedTextColor.AQUA)));
            return;
        }
        String target;
        if (sender instanceof Player player) {
            target = manager.getAlignmentFor(player);
        } else {
            sender.sendMessage(Component.text("Provide 'supply alliance <id>' as console.",
                    NamedTextColor.RED));
            return;
        }
        if (target == null) { sender.sendMessage(Component.text("You have no alignment.", NamedTextColor.GRAY)); return; }
        sender.sendMessage(Component.text("Supply points for " + target + ": ", NamedTextColor.GOLD)
                .append(Component.text(String.valueOf(supply.getSupplyPoints(target)), NamedTextColor.AQUA)));
    }

    // ---- admin tools (dispatched from /contracts admin) ------------------

    /** /contracts admin influence add|remove <alignment> <amount> */
    public void adminInfluence(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN)) { sender.sendMessage(denied()); return; }
        if (diplomacy == null) { sender.sendMessage(unavailable()); return; }
        if (args.length < 5) {
            sender.sendMessage(Component.text("Usage: /contracts admin influence <add|remove> <alignment> <amount>",
                    NamedTextColor.RED));
            return;
        }
        String op = args[3].toLowerCase();
        String alignment = args[4].toLowerCase();
        int amount = parseInt(args.length >= 6 ? args[5] : "0");
        int delta = op.equals("remove") ? -amount : amount;
        diplomacy.adminAdjustInfluence(alignment, delta);
        sender.sendMessage(Component.text("Adjusted " + alignment + " influence by " + delta
                + " (now " + diplomacy.getInfluence(alignment) + ").", NamedTextColor.GREEN));
    }

    /** /contracts admin sabotage reset <player> */
    public void adminSabotageReset(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN)) { sender.sendMessage(denied()); return; }
        if (sabotage == null) { sender.sendMessage(unavailable()); return; }
        if (args.length < 4) {
            sender.sendMessage(Component.text("Usage: /contracts admin sabotage reset <player>", NamedTextColor.RED));
            return;
        }
        Player target = Bukkit.getPlayer(args[4]);
        if (target == null) { sender.sendMessage(Component.text("Player not online.", NamedTextColor.RED)); return; }
        sabotage.resetCooldown(target.getUniqueId());
        sender.sendMessage(Component.text("Cleared sabotage cooldown for " + target.getName() + ".",
                NamedTextColor.GREEN));
    }

    /** /contracts admin emergency generate <template> | clear */
    public void adminEmergency(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN)) { sender.sendMessage(denied()); return; }
        if (dynamic == null) { sender.sendMessage(unavailable()); return; }
        String op = args.length >= 4 ? args[3].toLowerCase() : "";
        switch (op) {
            case "generate" -> {
                if (args.length < 5) {
                    sender.sendMessage(Component.text("Usage: /contracts admin emergency generate <template>",
                            NamedTextColor.RED));
                    return;
                }
                dynamic.generate(args[4].toLowerCase()).ifPresentOrElse(
                        id -> sender.sendMessage(Component.text("Generated " + id + ".", NamedTextColor.GREEN)),
                        () -> sender.sendMessage(Component.text("Generation refused (template, cap or endpoints).",
                                NamedTextColor.RED)));
            }
            case "clear" -> {
                int removed = dynamic.clearAll();
                sender.sendMessage(Component.text("Cleared " + removed + " dynamic contract(s).",
                        NamedTextColor.GREEN));
            }
            default -> sender.sendMessage(Component.text("Usage: /contracts admin emergency <generate|clear>",
                    NamedTextColor.RED));
        }
    }

    /** /contracts admin reload-diplomacy */
    public void adminReloadDiplomacy(CommandSender sender) {
        if (!sender.hasPermission(ADMIN)) { sender.sendMessage(denied()); return; }
        if (diplomacySettings == null) { sender.sendMessage(unavailable()); return; }
        diplomacySettings.load();
        sender.sendMessage(Component.text("Diplomacy settings reloaded.", NamedTextColor.GREEN));
    }

    private int parsePage(String raw) {
        try {
            return Math.max(1, Integer.parseInt(raw));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private int parseInt(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private Component unavailable() {
        return Component.text("The contract strategic layer is not available.", NamedTextColor.RED);
    }

    private Component denied() {
        return Component.text("You do not have permission.", NamedTextColor.RED);
    }
}
