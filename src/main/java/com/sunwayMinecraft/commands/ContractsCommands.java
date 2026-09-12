package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.contracts.domain.ActiveContract;
import com.sunwayMinecraft.contracts.domain.ContractDefinition;
import com.sunwayMinecraft.contracts.domain.ContractEndpoint;
import com.sunwayMinecraft.contracts.service.ContractsManager;
import com.sunwayMinecraft.contracts.service.ContractVerificationService;
import com.sunwayMinecraft.events.domain.CityEventDefinition;
import com.sunwayMinecraft.events.service.EventModifierService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class ContractsCommands implements CommandExecutor, TabCompleter {
    private static final int BOARD_PAGE_SIZE = 8;

    private final ContractsManager manager;
    private final ContractVerificationService verificationService;
    private EventModifierService eventModifierService;

    public ContractsCommands(ContractsManager manager, ContractVerificationService verificationService) {
        this.manager = manager;
        this.verificationService = verificationService;
    }

    public void setEventModifierService(EventModifierService eventModifierService) {
        this.eventModifierService = eventModifierService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use contract commands.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "board", "list" -> showBoard(player, args);
            case "accept" -> acceptContract(player, args);
            case "active" -> listActive(player);
            case "progress" -> showProgress(player, args);
            case "info" -> showInfo(player, args);
            case "complete" -> completeContract(player, args);
            case "abandon" -> abandonContract(player, args);
            case "admin" -> handleAdmin(player, args);
            case "help" -> sendHelp(player);
            default -> sendHelp(player);
        }

        return true;
    }

    private void showBoard(Player player, String[] args) {
        List<ContractDefinition> contracts =
                new ArrayList<>(manager.getContractConfig().getContracts().values());
        String filterDescription = null;

        // "/contracts board <campus>", or an alignment/type keyword filter with a
        // value; a trailing number selects the page
        int pageArg = args.length > 1 && args[args.length - 1].matches("\\d+")
                ? args.length - 1 : args.length;
        if (args.length >= 2 && !args[1].matches("\\d+")) {
            String keyword = args[1].toLowerCase(Locale.ROOT);
            if (keyword.equals("alignment") || keyword.equals("type")) {
                if (pageArg < 3) {
                    player.sendMessage(Component.text(
                            "Usage: /contracts board " + keyword + " <" + keyword + "> [page]",
                            NamedTextColor.RED));
                    return;
                }
                String value = args[2].toLowerCase(Locale.ROOT);
                if (keyword.equals("alignment")) {
                    contracts.removeIf(def -> !def.alignmentRule().canAccept(value));
                } else {
                    contracts.removeIf(def -> !def.category().name().equalsIgnoreCase(value));
                }
                filterDescription = keyword + " " + value;
            } else {
                String campus = args[1].toLowerCase(Locale.ROOT);
                contracts.removeIf(def -> !def.campusRoute().touchesCampus(campus));
                filterDescription = "campus " + campus;
            }
        }

        int totalPages = Math.max(1, (contracts.size() + BOARD_PAGE_SIZE - 1) / BOARD_PAGE_SIZE);
        int page = parsePage(pageArg < args.length ? args[pageArg] : "1", totalPages);

        player.sendMessage(Component.text("=== City Contracts Board (page " + page + "/"
                + totalPages + ")" + (filterDescription != null ? " [" + filterDescription + "]" : "")
                + " ===", NamedTextColor.GOLD, TextDecoration.BOLD));

        if (contracts.isEmpty()) {
            player.sendMessage(Component.text("No contracts match this filter.", NamedTextColor.GRAY));
            return;
        }

        int start = (page - 1) * BOARD_PAGE_SIZE;
        for (ContractDefinition def : contracts.subList(start,
                Math.min(start + BOARD_PAGE_SIZE, contracts.size()))) {
            player.sendMessage(boardEntry(def));
        }
        player.sendMessage(Component.text("Use /contracts info <id> for details.", NamedTextColor.GRAY));
    }

    /** One board line: name, type, route, required alignment and boosted status. */
    private Component boardEntry(ContractDefinition def) {
        Component msg = Component.text("- ", NamedTextColor.GRAY)
                .append(Component.text(def.name(), NamedTextColor.YELLOW))
                .append(Component.text(" [" + def.category().name() + "]", NamedTextColor.WHITE));

        if (def.campusRoute().originCampus() != null || def.campusRoute().destinationCampus() != null) {
            msg = msg.append(Component.text(" " + routeLabel(def.campusRoute()), NamedTextColor.DARK_AQUA));
        }
        if (def.alignmentRule().requiredAlignment() != null) {
            msg = msg.append(Component.text(" requires " + def.alignmentRule().requiredAlignment(),
                    NamedTextColor.LIGHT_PURPLE));
        }

        if (eventModifierService != null) {
            var eventOpt = eventModifierService.getPrimaryEventForCategory(def.category());
            if (eventOpt.isPresent()) {
                msg = msg.append(Component.text(" [BOOSTED: " + eventOpt.get().name() + "]",
                        NamedTextColor.AQUA, TextDecoration.BOLD));
            }
        }

        return msg.append(Component.text(" (ID: " + def.id() + ")", NamedTextColor.DARK_GRAY));
    }

    private String routeLabel(com.sunwayMinecraft.contracts.domain.ContractCampusRoute route) {
        String origin = route.originCampus() != null ? route.originCampus() : "?";
        String destination = route.destinationCampus() != null ? route.destinationCampus() : "?";
        return origin + " -> " + destination;
    }

    private int parsePage(String raw, int totalPages) {
        try {
            return Math.min(Math.max(1, Integer.parseInt(raw)), totalPages);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private void handleAdmin(Player player, String[] args) {
        if (!player.hasPermission("sunway.contracts.admin")) {
            player.sendMessage(Component.text("You do not have permission to use contract admin commands.",
                    NamedTextColor.RED));
            return;
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("list")) {
            showDisabledContracts(player);
            return;
        }
        player.sendMessage(Component.text("Usage: /contracts admin list", NamedTextColor.RED));
    }

    private void showDisabledContracts(Player player) {
        Map<String, String> reasons = manager.getContractConfig().getDisabledReasons();
        if (reasons.isEmpty()) {
            player.sendMessage(Component.text("No disabled contracts.", NamedTextColor.GRAY));
            return;
        }
        player.sendMessage(Component.text("=== Disabled Contracts ===", NamedTextColor.GOLD, TextDecoration.BOLD));
        for (Map.Entry<String, String> entry : reasons.entrySet()) {
            player.sendMessage(Component.text("- ", NamedTextColor.GRAY)
                    .append(Component.text(entry.getKey(), NamedTextColor.YELLOW))
                    .append(Component.text(": " + entry.getValue(), NamedTextColor.RED)));
        }
    }

    private void acceptContract(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /contracts accept <id>", NamedTextColor.RED));
            return;
        }
        String id = args[1];
        if (manager.acceptContract(player, id)) {
            player.sendMessage(Component.text("Contract accepted: " + id, NamedTextColor.GREEN));
        } else {
            String problem = manager.acceptanceProblem(player, id);
            player.sendMessage(Component.text(
                    problem != null ? problem : "Failed to accept contract.", NamedTextColor.RED));
        }
    }

    private void listActive(Player player) {
        List<ActiveContract> active = manager.getPersistence().getPlayerContracts(player.getUniqueId());
        if (active.isEmpty()) {
            player.sendMessage(Component.text("You have no active contracts.", NamedTextColor.YELLOW));
            return;
        }
        player.sendMessage(Component.text("=== Your Active Contracts ===", NamedTextColor.GOLD, TextDecoration.BOLD));
        for (ActiveContract ac : active) {
            ContractDefinition def = manager.getContractConfig().getContract(ac.getContractId());
            long minsLeft = Duration.between(Instant.now(), ac.getExpiryTime()).toMinutes();
            Component msg = Component.text("- ", NamedTextColor.GRAY)
                .append(Component.text(def != null ? def.name() : ac.getContractId(), NamedTextColor.YELLOW))
                .append(Component.text(" (" + Math.max(0, minsLeft) + "m left)", NamedTextColor.GRAY));
            
            if (def != null && eventModifierService != null) {
                var eventOpt = eventModifierService.getPrimaryEventForCategory(def.category());
                if (eventOpt.isPresent()) {
                    msg = msg.append(Component.text(" [BOOSTED]", NamedTextColor.AQUA, TextDecoration.BOLD));
                }
            }
            player.sendMessage(msg);
        }
        player.sendMessage(Component.text("Use /contracts progress [id] for more details.", NamedTextColor.GRAY));
    }

    private void showProgress(Player player, String[] args) {
        List<ActiveContract> active = manager.getPersistence().getPlayerContracts(player.getUniqueId());
        if (active.isEmpty()) {
            player.sendMessage(Component.text("You have no active contracts.", NamedTextColor.YELLOW));
            return;
        }

        if (args.length >= 2) {
            String id = args[1];
            active.stream().filter(ac -> ac.getContractId().equalsIgnoreCase(id)).findFirst().ifPresentOrElse(ac -> {
                showContractProgress(player, ac);
            }, () -> {
                player.sendMessage(Component.text("Contract not found in your active list.", NamedTextColor.RED));
            });
        } else {
            player.sendMessage(Component.text("=== Your Contract Progress ===", NamedTextColor.GOLD, TextDecoration.BOLD));
            active.forEach(ac -> showContractProgress(player, ac));
        }
    }

    private void showContractProgress(Player player, ActiveContract ac) {
        ContractDefinition def = manager.getContractConfig().getContract(ac.getContractId());
        if (def == null) return;

        long minsLeft = Duration.between(Instant.now(), ac.getExpiryTime()).toMinutes();
        Component msg = Component.text("- ", NamedTextColor.GRAY)
                .append(Component.text(def.name(), NamedTextColor.YELLOW))
                .append(Component.text(" [" + Math.max(0, minsLeft) + "m left]", NamedTextColor.GRAY));
        
        player.sendMessage(msg);
        player.sendMessage(Component.text("  Objective: ", NamedTextColor.DARK_GRAY).append(Component.text(def.objectiveDescription(), NamedTextColor.WHITE)));
        
        // Show if it's currently boosted
        if (eventModifierService != null) {
            eventModifierService.getPrimaryEventForCategory(def.category()).ifPresent(event -> {
                player.sendMessage(Component.text("  [Active Boost: " + event.name() + " x" + event.rewardMultiplier() + "]", NamedTextColor.AQUA));
            });
        }
    }

    private void showInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /contracts info <id>", NamedTextColor.RED));
            return;
        }
        ContractDefinition def = manager.getContractConfig().getContract(args[1]);
        if (def == null) {
            player.sendMessage(Component.text("Contract not found.", NamedTextColor.RED));
            return;
        }
        player.sendMessage(Component.text("=== Contract: " + def.name() + " ===", NamedTextColor.GOLD, TextDecoration.BOLD));
        player.sendMessage(Component.text("ID: ", NamedTextColor.YELLOW).append(Component.text(def.id(), NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Category: ", NamedTextColor.YELLOW).append(Component.text(def.category().name(), NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Description: ", NamedTextColor.YELLOW).append(Component.text(def.description(), NamedTextColor.WHITE)));
        
        double reward = def.rewardMoney();
        
        if (eventModifierService != null) {
            eventModifierService.getPrimaryEventForCategory(def.category()).ifPresentOrElse(event -> {
                double boostedReward = reward * event.rewardMultiplier();
                player.sendMessage(Component.text("Reward: ", NamedTextColor.YELLOW)
                    .append(Component.text("$" + reward, NamedTextColor.GRAY, TextDecoration.STRIKETHROUGH))
                    .append(Component.text(" -> ", NamedTextColor.GRAY))
                    .append(Component.text("$" + boostedReward, NamedTextColor.GREEN, TextDecoration.BOLD)));
                player.sendMessage(Component.text("Boosted by: ", NamedTextColor.AQUA)
                    .append(Component.text(event.name() + " (x" + event.rewardMultiplier() + ")", NamedTextColor.WHITE)));
            }, () -> {
                player.sendMessage(Component.text("Reward: ", NamedTextColor.YELLOW).append(Component.text("$" + reward, NamedTextColor.GREEN)));
            });
        } else {
            player.sendMessage(Component.text("Reward: ", NamedTextColor.YELLOW).append(Component.text("$" + reward, NamedTextColor.GREEN)));
        }

        player.sendMessage(Component.text("Objective: ", NamedTextColor.YELLOW).append(Component.text(def.objectiveDescription(), NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Duration: ", NamedTextColor.YELLOW).append(Component.text(def.durationMinutes() + " minutes", NamedTextColor.WHITE)));

        var route = def.campusRoute();
        if (route.originCampus() != null || route.destinationCampus() != null) {
            player.sendMessage(Component.text("Route: ", NamedTextColor.YELLOW)
                    .append(Component.text(routeLabel(route), NamedTextColor.WHITE)));
        }
        var rule = def.alignmentRule();
        if (rule.requiredAlignment() != null) {
            player.sendMessage(Component.text("Required alignment: ", NamedTextColor.YELLOW)
                    .append(Component.text(rule.requiredAlignment(), NamedTextColor.WHITE)));
        }
        if (rule.recommendedAlignment() != null) {
            player.sendMessage(Component.text("Recommended for: ", NamedTextColor.YELLOW)
                    .append(Component.text(rule.recommendedAlignment(), NamedTextColor.WHITE)));
        }
        if (!rule.forbiddenAlignments().isEmpty()) {
            player.sendMessage(Component.text("Forbidden for: ", NamedTextColor.YELLOW)
                    .append(Component.text(String.join(", ", rule.forbiddenAlignments()), NamedTextColor.WHITE)));
        }
        if (def.rewardReputation() > 0) {
            player.sendMessage(Component.text("Reputation reward: ", NamedTextColor.YELLOW)
                    .append(Component.text(String.valueOf(def.rewardReputation()), NamedTextColor.WHITE)));
        }
    }

    private void completeContract(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /contracts complete <id>", NamedTextColor.RED));
            return;
        }
        String id = args[1];
        ActiveContract ac = manager.getPersistence().getPlayerContracts(player.getUniqueId()).stream()
            .filter(a -> a.getContractId().equals(id)).findFirst().orElse(null);

        if (ac == null) {
            player.sendMessage(Component.text("You don't have this contract active.", NamedTextColor.RED));
            return;
        }

        ContractVerificationService.VerificationResult result = verificationService.verifyCompletion(player, ac);
        if (result.success()) {
            ContractDefinition def = manager.getContractConfig().getContract(ac.getContractId());
            double reward = def != null ? def.rewardMoney() : 0;
            if (eventModifierService != null && def != null) {
                reward *= eventModifierService.getRewardMultiplier(def.category());
            }

            if (manager.completeContract(player, ac)) {
                player.sendMessage(Component.text("Contract completed! ", NamedTextColor.GREEN)
                        .append(Component.text("Reward: $" + reward, NamedTextColor.GOLD, TextDecoration.BOLD)));
            }
        } else {
            player.sendMessage(Component.text("Cannot complete: " + result.message(), NamedTextColor.RED));
        }
    }

    private void abandonContract(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /contracts abandon <id>", NamedTextColor.RED));
            return;
        }
        String id = args[1];
        ActiveContract ac = manager.getPersistence().getPlayerContracts(player.getUniqueId()).stream()
            .filter(a -> a.getContractId().equals(id)).findFirst().orElse(null);

        if (ac == null) {
            player.sendMessage(Component.text("You don't have this contract active.", NamedTextColor.RED));
            return;
        }

        manager.abandonContract(player, ac);
        player.sendMessage(Component.text("Contract abandoned. Cooldown applied.", NamedTextColor.YELLOW));
    }

    private void sendHelp(Player player) {
        player.sendMessage(Component.text("=== City Contracts Help ===", NamedTextColor.GOLD, TextDecoration.BOLD));
        player.sendMessage(Component.text("/contracts board [campus] - View available contracts", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/contracts board alignment <id> - Contracts your alignment can accept", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/contracts board type <type> - Filter by contract type", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/contracts info <id> - View contract details", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/contracts accept <id> - Accept a contract", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/contracts active - List your active contracts", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/contracts progress [id] - Show detailed progress", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/contracts complete <id> - Complete a contract", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/contracts abandon <id> - Abandon a contract", NamedTextColor.YELLOW));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return null;
        if (args.length == 1) {
            return List.of("board", "list", "accept", "active", "progress", "info", "complete",
                    "abandon", "admin", "help");
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("accept") || args[0].equalsIgnoreCase("info")) {
                return new ArrayList<>(manager.getContractConfig().getContracts().keySet());
            }
            if (args[0].equalsIgnoreCase("complete") || args[0].equalsIgnoreCase("abandon") || args[0].equalsIgnoreCase("progress")) {
                return manager.getPersistence().getPlayerContracts(player.getUniqueId()).stream()
                    .map(ActiveContract::getContractId).collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("admin") && player.hasPermission("sunway.contracts.admin")) {
                return List.of("list");
            }
            if (args[0].equalsIgnoreCase("board") || args[0].equalsIgnoreCase("list")) {
                List<String> options = new ArrayList<>(List.of("alignment", "type"));
                for (com.sunwayMinecraft.alignments.domain.Campus campus :
                        com.sunwayMinecraft.alignments.domain.Campus.values()) {
                    options.add(campus.getId());
                }
                return options;
            }
        }
        if (args.length == 3 && (args[0].equalsIgnoreCase("board") || args[0].equalsIgnoreCase("list"))) {
            if (args[1].equalsIgnoreCase("alignment")) {
                return manager.getContractConfig().getContracts().values().stream()
                        .map(def -> def.alignmentRule().requiredAlignment())
                        .filter(id -> id != null).distinct().collect(Collectors.toList());
            }
            if (args[1].equalsIgnoreCase("type")) {
                return java.util.Arrays.stream(com.sunwayMinecraft.contracts.domain.ContractCategory.values())
                        .map(category -> category.name().toLowerCase(Locale.ROOT))
                        .collect(Collectors.toList());
            }
        }
        return null;
    }
}
