package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.alignments.config.AlignmentPerksConfig;
import com.sunwayMinecraft.alignments.config.AlignmentProgressionConfig;
import com.sunwayMinecraft.alignments.domain.AlignmentMembership;
import com.sunwayMinecraft.alignments.service.AlignmentPerkService;
import com.sunwayMinecraft.alignments.service.AlignmentRankService;
import com.sunwayMinecraft.alignments.service.AlignmentResult;
import com.sunwayMinecraft.alignments.service.AlignmentScoreService;
import com.sunwayMinecraft.alignments.service.AlignmentSeasonService;
import com.sunwayMinecraft.alignments.service.AlignmentService;
import com.sunwayMinecraft.alignments.service.AlignmentSeasonService;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AlignProgressionCommandsTest {
    private ServerMock server;
    private PlayerMock admin;
    private AlignmentService service;
    private AlignmentSeasonService seasonService;
    private AlignmentScoreService scoreService;
    private AlignmentPerksConfig perksConfig;
    private AlignProgressionCommands commands;

    private final UUID playerUuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        admin = server.addPlayer("Admin");
        admin.setOp(true);
        service = mock(AlignmentService.class);
        seasonService = mock(AlignmentSeasonService.class);
        scoreService = mock(AlignmentScoreService.class);
        perksConfig = mock(AlignmentPerksConfig.class);
        when(service.isAvailable()).thenReturn(true);
        commands = new AlignProgressionCommands(
                service,
                mock(AlignmentProgressionConfig.class),
                perksConfig,
                mock(AlignmentRankService.class),
                seasonService,
                scoreService);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void reputationAddRemoveAndSetDelegateToTheService() {
        PlayerMock target = server.addPlayer("Target");
        when(service.adjustReputation(target.getUniqueId(), 50))
                .thenReturn(AlignmentResult.JOINED);
        when(service.adjustReputation(target.getUniqueId(), -25))
                .thenReturn(AlignmentResult.JOINED);
        when(service.setReputation(target.getUniqueId(), 100))
                .thenReturn(AlignmentResult.JOINED);
        when(service.getMembership(target.getUniqueId())).thenReturn(Optional.of(
                new AlignmentMembership(target.getUniqueId(), "azure_hearth", 1000L, 100, "active")));

        call("reputation", "add", "Target", "50");
        call("reputation", "remove", "Target", "25");
        call("reputation", "set", "Target", "100");

        verify(service).adjustReputation(target.getUniqueId(), 50);
        verify(service).adjustReputation(target.getUniqueId(), -25);
        verify(service).setReputation(target.getUniqueId(), 100);
        assertTrue(drainMessages(admin).stream()
                .anyMatch(msg -> msg.contains("Reputation of §fTarget")));
    }

    @Test
    void reputationCommandsRejectUnalignedPlayersUnknownPlayersAndNonAdmins() {
        CommandSender nonAdmin = mock(CommandSender.class);
        when(nonAdmin.hasPermission("sunway.align.admin")).thenReturn(false);
        commands.handleReputation(nonAdmin, new String[]{"reputation", "add", "x", "1"});
        verify(nonAdmin).sendMessage(org.mockito.ArgumentMatchers.argThat(
                (String message) -> message != null && message.contains("permission")));
        verify(service, never()).adjustReputation(any(UUID.class), org.mockito.ArgumentMatchers.anyInt());

        server.addPlayer("Target");
        when(service.adjustReputation(any(UUID.class), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(AlignmentResult.NOT_ALIGNED);
        call("reputation", "add", "Target", "10");
        assertTrue(drainMessages(admin).stream()
                .anyMatch(msg -> msg.contains("not aligned")));

        call("reputation", "add", "Ghost", "10");
        assertTrue(drainMessages(admin).stream()
                .anyMatch(msg -> msg.contains("Player not found")));
    }

    @Test
    void leaderboardViewsRenderAllianceScoresAndSeasonLeaders() {
        when(scoreService.getGrandAllianceScores()).thenReturn(java.util.Map.of(
                "concordat_of_the_dawn", 420L,
                "ironclad_syndicate", 108L));
        call("leaderboard", "grand");
        assertTrue(drainMessages(admin).stream()
                .anyMatch(msg -> msg.contains("concordat_of_the_dawn")), "grand scores expected");

        when(seasonService.getCurrentSeason())
                .thenReturn(Optional.of(new com.sunwayMinecraft.alignments.persistence.AlignmentSeasonRepository.SeasonState(
                        "season-2", System.currentTimeMillis())));
        when(seasonService.getSeasonAlignmentTotals()).thenReturn(Map.of(
                "azure_hearth", new long[] {150, 3}));
        call("leaderboard", "season");
        assertTrue(drainMessages(admin).stream()
                .anyMatch(msg -> msg.contains("azure_hearth")), "season leaders expected");

        when(service.getAlignmentTotals()).thenReturn(Map.of(
                "azure_hearth", new long[] {200, 5}));
        call("leaderboard");
        assertTrue(drainMessages(admin).stream()
                .anyMatch(msg -> msg.contains("Alignment Reputation")));
    }

    @Test
    void playerStandingReportsUnalignedPlayers() {
        PlayerMock target = server.addPlayer("Target");
        when(service.getAlignmentId(target.getUniqueId())).thenReturn(Optional.empty());

        call("leaderboard", "player", "Target");
        assertTrue(drainMessages(admin).stream()
                .anyMatch(msg -> msg.contains("unaligned - no standing")));
    }

    @Test
    void seasonCommandsRequireAdminAndReportResults() {
        when(seasonService.endSeason(org.mockito.ArgumentMatchers.anyString())).thenReturn(true);
        when(seasonService.resetSeason()).thenReturn(true);
        when(seasonService.forceSnapshot()).thenReturn(true);
        when(seasonService.getCurrentSeason())
                .thenReturn(Optional.of(new com.sunwayMinecraft.alignments.persistence.AlignmentSeasonRepository.SeasonState(
                        "season-2", System.currentTimeMillis())));

        call("season", "end");
        call("season", "reset");
        call("season", "snapshot");
        call("season", "info");

        verify(seasonService).endSeason("admin command");
        verify(seasonService).resetSeason();
        verify(seasonService).forceSnapshot();
        List<String> messages = drainMessages(admin);
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("next season started")));
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("Season season-2")));
    }

    @Test
    void perksReloadReloadsThePerkConfiguration() {
        when(perksConfig.getPerks()).thenReturn(Map.of());
        call("perks", "reload");
        verify(perksConfig).load();
        assertTrue(drainMessages(admin).stream()
                .anyMatch(msg -> msg.contains("perks configuration reloaded")));
    }

    private void call(String... args) {
        switch (args[0]) {
            case "leaderboard" -> commands.handleLeaderboard(admin, args);
            case "season" -> commands.handleSeason(admin, args);
            case "reputation" -> commands.handleReputation(admin, args);
            case "perks" -> commands.handlePerks(admin, args);
            default -> throw new IllegalArgumentException(args[0]);
        }
    }

    private List<String> drainMessages(PlayerMock recipient) {
        List<String> messages = new ArrayList<>();
        String message;
        while ((message = recipient.nextMessage()) != null) {
            messages.add(message);
        }
        return messages;
    }
}
