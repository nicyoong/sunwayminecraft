package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.alignments.config.AlignmentConfigManager;
import com.sunwayMinecraft.alignments.config.AlignmentPerksConfig;
import com.sunwayMinecraft.alignments.config.AlignmentProgressionConfig;
import com.sunwayMinecraft.alignments.config.AlignmentSettingsConfig;
import com.sunwayMinecraft.alignments.domain.AlignmentDefinition;
import com.sunwayMinecraft.alignments.domain.AlignmentMembership;
import com.sunwayMinecraft.alignments.domain.Campus;
import com.sunwayMinecraft.alignments.domain.GrandAlliance;
import com.sunwayMinecraft.alignments.domain.GrandAllianceDefinition;
import com.sunwayMinecraft.alignments.service.AlignmentChatService;
import com.sunwayMinecraft.alignments.service.AlignmentMembershipCache;
import com.sunwayMinecraft.alignments.service.AlignmentMembershipCache.CachedMembership;
import com.sunwayMinecraft.alignments.service.AlignmentResult;
import com.sunwayMinecraft.alignments.service.AlignmentService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlignCommandsTest {
    private ServerMock server;
    private PlayerMock player;
    private AlignmentService service;
    private AlignmentConfigManager configManager;
    private AlignmentSettingsConfig settings;
    private AlignmentChatService chatService;
    private AlignmentMembershipCache cache;
    private AlignCommands commands;
    private AlignTabCompleter tabCompleter;

    private final AlignmentDefinition azureHearth = new AlignmentDefinition(
            "azure_hearth", "Azure Hearth", GrandAlliance.CONCORDAT_OF_THE_DAWN,
            Campus.TAYLORS, "Keepers of the flame", "[Azure Hearth]", "&9", true);

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        player = server.addPlayer();
        service = mock(AlignmentService.class);
        configManager = mock(AlignmentConfigManager.class);
        settings = mock(AlignmentSettingsConfig.class);
        chatService = mock(AlignmentChatService.class);
        cache = mock(AlignmentMembershipCache.class);
        when(service.isAvailable()).thenReturn(true);
        when(settings.isAllowAdminBypass()).thenReturn(true);
        when(settings.isShowJoinMessage()).thenReturn(true);
        when(settings.isShowLeaveMessage()).thenReturn(true);
        when(service.getAlignmentMemberCounts()).thenReturn(Map.of());
        when(configManager.getAlignment("azure_hearth")).thenReturn(Optional.of(azureHearth));
        when(configManager.getEnabledAlignments()).thenReturn(List.of(azureHearth));
        when(configManager.getAlignmentsByGrandAlliance(GrandAlliance.CONCORDAT_OF_THE_DAWN))
                .thenReturn(List.of(azureHearth));
        when(configManager.getAlignmentsByGrandAlliance(GrandAlliance.IRONCLAD_SYNDICATE))
                .thenReturn(List.of());
        when(configManager.getAllianceDefinition(GrandAlliance.CONCORDAT_OF_THE_DAWN))
                .thenReturn(Optional.of(new GrandAllianceDefinition(
                        "concordat_of_the_dawn", "Concordat of the Dawn", "desc", "&b")));
        commands = new AlignCommands(service, configManager, settings, chatService, cache,
            new AlignProgressionCommands(service, mock(AlignmentProgressionConfig.class),
                mock(AlignmentPerksConfig.class),
                mock(com.sunwayMinecraft.alignments.service.AlignmentRankService.class),
                mock(com.sunwayMinecraft.alignments.service.AlignmentSeasonService.class),
                mock(com.sunwayMinecraft.alignments.service.AlignmentScoreService.class)));
        tabCompleter = new AlignTabCompleter(configManager);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void listShowsAllianceGroupingMemberCountsAndPageHeader() {
        when(service.getAlignmentMemberCounts()).thenReturn(Map.of("azure_hearth", 3));

        assertTrue(commands.onCommand(player, command("align"), "align", new String[]{"list"}));

        List<String> messages = drainMessages(player);
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("page 1/1")), messages.toString());
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("Concordat of the Dawn")),
                messages.toString());
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("Azure Hearth")), messages.toString());
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("3 members")), messages.toString());
    }

    @Test
    void joinConfirmsWithAllianceCampusAndBroadcastsToOtherPlayers() {
        PlayerMock spectator = server.addPlayer("Spectator");
        when(service.join(player.getUniqueId(), "azure_hearth", false))
                .thenReturn(AlignmentResult.JOINED);

        assertTrue(commands.onCommand(player, command("align"), "align",
                new String[]{"join", "azure", "hearth"}));

        verify(service).join(player.getUniqueId(), "azure_hearth", false);
        List<String> messages = drainMessages(player);
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("Azure Hearth")), messages.toString());
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("Concordat of the Dawn")),
                messages.toString());
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("taylors")), messages.toString());

        assertTrue(drainMessages(spectator).stream()
                        .anyMatch(msg -> msg.contains("has pledged to §fAzure Hearth")),
                "join broadcast must reach other online players");
    }

    @Test
    void joinShowsHumanReadableRemainingCooldown() {
        when(service.join(player.getUniqueId(), "azure_hearth", false))
                .thenReturn(AlignmentResult.COOLDOWN_ACTIVE);
        when(service.getRemainingCooldownSeconds(player.getUniqueId())).thenReturn(5400L);

        assertTrue(commands.onCommand(player, command("align"), "align",
                new String[]{"join", "azure_hearth"}));

        assertTrue(drainMessages(player).stream().anyMatch(msg -> msg.contains("1h 30m")),
                "cooldown message must be human readable");
    }

    @Test
    void joinRequiresAnAvailableSystemAndAnAlignmentName() {
        when(service.isAvailable()).thenReturn(false);
        assertTrue(commands.onCommand(player, command("align"), "align",
                new String[]{"join", "azure_hearth"}));
        assertTrue(drainMessages(player).stream().anyMatch(msg -> msg.contains("unavailable")));

        when(service.isAvailable()).thenReturn(true);
        assertTrue(commands.onCommand(player, command("align"), "align", new String[]{"join"}));
        assertTrue(drainMessages(player).stream().anyMatch(msg -> msg.contains("Usage")));
        verify(service, never()).join(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void leaveClearsMembershipAndBroadcastsDeparture() {
        PlayerMock spectator = server.addPlayer("Spectator");
        when(service.leave(player.getUniqueId())).thenReturn(AlignmentResult.LEFT);

        assertTrue(commands.onCommand(player, command("align"), "align", new String[]{"leave"}));

        verify(service).leave(player.getUniqueId());
        assertTrue(drainMessages(player).stream().anyMatch(msg -> msg.contains("left your alignment")));
        assertTrue(drainMessages(spectator).stream().anyMatch(msg -> msg.contains("is now unaligned")));
    }

    @Test
    void showReadsFromTheCacheFirst() {
        when(cache.getOrLoad(player.getUniqueId())).thenReturn(Optional.of(new CachedMembership(
                player.getUniqueId(), "azure_hearth",
                GrandAlliance.CONCORDAT_OF_THE_DAWN.getId(), Campus.TAYLORS.getId(),
                7, "active", System.currentTimeMillis())));
        when(service.getMembership(player.getUniqueId())).thenReturn(Optional.of(
                AlignmentMembership.newMembership(player.getUniqueId(), "azure_hearth", 1000L)));

        assertTrue(commands.onCommand(player, command("align"), "align", new String[]{"show"}));

        List<String> messages = drainMessages(player);
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("Azure Hearth")), messages.toString());
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("Concordat of the Dawn")),
                messages.toString());
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("Joined:")), messages.toString());
        verify(cache).getOrLoad(player.getUniqueId());
    }

    @Test
    void showReportsUnalignedPlayersAndGuardsOtherPlayerLookups() {
        when(cache.getOrLoad(player.getUniqueId())).thenReturn(Optional.empty());
        assertTrue(commands.onCommand(player, command("align"), "align", new String[]{"show"}));
        assertTrue(drainMessages(player).stream().anyMatch(msg -> msg.contains("Unaligned")));

        CommandSender nonAdmin = mock(CommandSender.class);
        when(nonAdmin.hasPermission("sunway.align.admin")).thenReturn(false);
        assertTrue(commands.onCommand(nonAdmin, command("align"), "align",
                new String[]{"show", "Someone"}));
        verify(nonAdmin).sendMessage(org.mockito.ArgumentMatchers.argThat(
                (String message) -> message != null && message.contains("permission")));
    }

    @Test
    void chatDelegatesToTheAlignmentChatService() {
        when(chatService.sendAlignmentChat(player, "hello world"))
                .thenReturn(AlignmentChatService.ChatResult.SENT);
        assertTrue(commands.onCommand(player, command("align"), "align",
                new String[]{"chat", "hello", "world"}));

        when(chatService.sendAlignmentChat(player, "hi"))
                .thenReturn(AlignmentChatService.ChatResult.NO_ALIGNMENT);
        assertTrue(commands.onCommand(player, command("align"), "align",
                new String[]{"chat", "hi"}));
        assertTrue(drainMessages(player).stream().anyMatch(msg -> msg.contains("join an alignment")));

        // /ac routes every argument as message content
        assertTrue(commands.onCommand(player, command("ac"), "ac", new String[]{"hello", "world"}));
        verify(chatService, org.mockito.Mockito.times(2))
                .sendAlignmentChat(player, "hello world");
    }

    @Test
    void chatspyRequiresItsPermissionAndToggles() {
        assertTrue(commands.onCommand(player, command("align"), "align", new String[]{"chatspy"}));
        assertTrue(drainMessages(player).stream().anyMatch(msg -> msg.contains("permission")));
        verify(chatService, never()).toggleSpy(player.getUniqueId());

        player.setOp(true);
        when(chatService.toggleSpy(player.getUniqueId())).thenReturn(true);
        assertTrue(commands.onCommand(player, command("align"), "align", new String[]{"chatspy"}));
        verify(chatService).toggleSpy(player.getUniqueId());
        assertTrue(drainMessages(player).stream().anyMatch(msg -> msg.contains("enabled")));
    }

    @Test
    void unknownSubcommandsFallBackToHelp() {
        assertTrue(commands.onCommand(player, command("align"), "align", new String[]{"warp"}));
        assertTrue(drainMessages(player).stream().anyMatch(msg -> msg.contains("Unknown subcommand")));
    }

    @Test
    void tabCompletionCoversSubcommandsAlignmentIdsAndPlayerNames() {
        assertEquals(List.of("help", "list", "join", "leave", "show", "chat"),
                tabCompleter.onTabComplete(player, command("align"), "align", new String[]{""}));

        assertEquals(List.of("azure_hearth"),
                tabCompleter.onTabComplete(player, command("align"), "align",
                        new String[]{"join", ""}));
        assertEquals(List.of(),
                tabCompleter.onTabComplete(player, command("align"), "align",
                        new String[]{"chat", "partial message"}));

        CommandSender admin = mock(CommandSender.class);
        when(admin.hasPermission("sunway.align.admin")).thenReturn(true);
        server.addPlayer("SpectatorPlayer");
        assertTrue(tabCompleter
                        .onTabComplete(admin, command("align"), "align", new String[]{"set", ""})
                        .contains("SpectatorPlayer"),
                "set must tab-complete online player names");
    }

    private List<String> drainMessages(PlayerMock recipient) {
        List<String> messages = new ArrayList<>();
        String message;
        while ((message = recipient.nextMessage()) != null) {
            messages.add(message);
        }
        return messages;
    }

    private Command command(String name) {
        Command command = mock(Command.class);
        when(command.getName()).thenReturn(name);
        return command;
    }

    @Test
    void everySubcommandRoutesToItsCollaboratorWithoutUnknownMessages() {
        when(service.join(org.mockito.ArgumentMatchers.eq(player.getUniqueId()),
                org.mockito.ArgumentMatchers.eq("azure_hearth"),
                org.mockito.ArgumentMatchers.anyBoolean()))
                .thenReturn(AlignmentResult.JOINED);
        when(service.leave(player.getUniqueId())).thenReturn(AlignmentResult.LEFT);
        when(cache.getOrLoad(player.getUniqueId())).thenReturn(Optional.empty());
        when(chatService.sendAlignmentChat(player, "hello")).thenReturn(AlignmentChatService.ChatResult.SENT);
        when(chatService.toggleSpy(player.getUniqueId())).thenReturn(true);
        when(service.adjustReputation(player.getUniqueId(), 5)).thenReturn(AlignmentResult.JOINED);
        when(service.adminSet(any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(AlignmentResult.JOINED);
        when(service.adminClear(any())).thenReturn(AlignmentResult.LEFT);
        when(service.getMembership(player.getUniqueId())).thenReturn(Optional.empty());
        player.setOp(true);
        PlayerMock adminTarget = server.addPlayer("Admin"); // target for set/clear/info/reputation

        String[][] calls = {
                {"help"}, {"list"}, {"join", "azure_hearth"}, {"leave"},
                {"show"}, {"chat", "hello"}, {"chatspy"},
                {"set", "Admin", "azure_hearth"}, {"clear", "Admin"},
                {"info", "Admin"}, {"reload"},
                {"leaderboard"}, {"season", "info"}, {"reputation", "add", "Admin", "5"},
                {"perks", "reload"}};
        for (String[] callArgs : calls) {
            drainMessages(player);
            assertTrue(commands.onCommand(player, command("align"), "align", callArgs),
                    "onCommand must return true for " + java.util.Arrays.toString(callArgs));
            assertTrue(drainMessages(player).stream()
                            .noneMatch(msg -> msg.contains("Unknown subcommand")),
                    "subcommand must be routed: " + java.util.Arrays.toString(callArgs));
        }
        verify(service).adjustReputation(adminTarget.getUniqueId(), 5);
        verify(service).adminSet(adminTarget.getUniqueId(), "azure_hearth");
        verify(service).adminClear(adminTarget.getUniqueId());
        verify(chatService).toggleSpy(player.getUniqueId());
    }

    @Test
    void consoleIsRejectedForPlayerOnlySubcommandsWithUsageHints() {
        CommandSender console = mock(CommandSender.class);
        assertTrue(commands.onCommand(console, command("align"), "align", new String[]{"join", "azure_hearth"}));
        verify(console).sendMessage(org.mockito.ArgumentMatchers.argThat(
                (String m) -> m != null && m.contains("Only players can join")));
        assertTrue(commands.onCommand(console, command("align"), "align", new String[]{"leave"}));
        verify(console).sendMessage(org.mockito.ArgumentMatchers.argThat(
                (String m) -> m != null && m.contains("Only players can leave")));
        assertTrue(commands.onCommand(console, command("align"), "align", new String[]{"chat"}));
        verify(console).sendMessage(org.mockito.ArgumentMatchers.argThat(
                (String m) -> m != null && m.contains("Only players can use alignment chat")));
        assertTrue(commands.onCommand(console, command("ac"), "ac", new String[0]));
        verify(console, org.mockito.Mockito.times(2)).sendMessage(org.mockito.ArgumentMatchers.argThat(
                (String m) -> m != null && m.contains("Only players can use alignment chat")));
    }

    @Test
    void alignmentChatReportsDisabledAndEmptyStates() {
        when(chatService.sendAlignmentChat(org.mockito.ArgumentMatchers.eq(player), org.mockito.ArgumentMatchers.any()))
                .thenReturn(AlignmentChatService.ChatResult.DISABLED, AlignmentChatService.ChatResult.EMPTY_MESSAGE);

        assertTrue(commands.onCommand(player, command("align"), "align", new String[]{"chat", "x"}));
        assertTrue(drainMessages(player).stream().anyMatch(msg -> msg.contains("disabled")));
        assertTrue(commands.onCommand(player, command("align"), "align", new String[]{"chat", " "}));
        assertTrue(drainMessages(player).stream().anyMatch(msg -> msg.contains("Usage")));
    }

    @Test
    void adminTabCompletionExposesAdminSubcommandsAndViews() {
        CommandSender admin = mock(CommandSender.class);
        when(admin.hasPermission("sunway.align.admin")).thenReturn(true);
        when(admin.hasPermission("sunway.align.chatspy")).thenReturn(true);

        List<String> top = tabCompleter.onTabComplete(admin, command("align"), "align", new String[]{""});
        assertTrue(top.containsAll(List.of("set", "clear", "info", "reload", "chatspy")),
                "admin subcommands must complete for admins: " + top);

        List<String> views = tabCompleter.onTabComplete(admin, command("align"), "align",
                new String[]{"leaderboard", ""});
        assertEquals(List.of("alignments", "grand", "season", "player", "reload"), views);

        List<String> season = tabCompleter.onTabComplete(admin, command("align"), "align",
                new String[]{"season", ""});
        assertEquals(List.of("info", "end", "reset", "snapshot"), season);
    }
}