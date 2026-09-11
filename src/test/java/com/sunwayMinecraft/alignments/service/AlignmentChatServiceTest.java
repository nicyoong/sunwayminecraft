package com.sunwayMinecraft.alignments.service;

import com.sunwayMinecraft.alignments.config.AlignmentConfigManager;
import com.sunwayMinecraft.alignments.config.AlignmentSettingsConfig;
import com.sunwayMinecraft.alignments.domain.AlignmentDefinition;
import com.sunwayMinecraft.alignments.domain.Campus;
import com.sunwayMinecraft.alignments.domain.GrandAlliance;
import com.sunwayMinecraft.alignments.service.AlignmentMembershipCache.CachedMembership;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AlignmentChatServiceTest {
    private AlignmentSettingsConfig settings;
    private AlignmentConfigManager configManager;
    private AlignmentMembershipCache cache;
    private AlignmentChatService chatService;
    private org.mockbukkit.mockbukkit.ServerMock server;

    private PlayerMock lagoonMember;
    private PlayerMock lagoonMate;
    private PlayerMock azureMember;
    private PlayerMock spyAdmin;

    private final AlignmentDefinition lagoonCovenant = new AlignmentDefinition(
            "lagoon_covenant", "Lagoon Covenant", GrandAlliance.CONCORDAT_OF_THE_DAWN,
            Campus.SUNWAY, "desc", "[Lagoon Covenant]", "&3", true);
    private final AlignmentDefinition azureHearth = new AlignmentDefinition(
            "azure_hearth", "Azure Hearth", GrandAlliance.CONCORDAT_OF_THE_DAWN,
            Campus.TAYLORS, "desc", "[Azure Hearth]", "&9", true);

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        settings = mock(AlignmentSettingsConfig.class);
        configManager = mock(AlignmentConfigManager.class);
        cache = mock(AlignmentMembershipCache.class);
        when(settings.isAllowAlignmentChat()).thenReturn(true);
        when(settings.getAlignmentChatFormat()).thenReturn(
                "&7[{alignment}&7] &f{player}&7: &f{message}");
        when(settings.getAlignmentChatColor()).thenReturn("&b");
        when(configManager.getAlignment("lagoon_covenant")).thenReturn(Optional.of(lagoonCovenant));
        when(configManager.getAlignment("azure_hearth")).thenReturn(Optional.of(azureHearth));
        chatService = new AlignmentChatService(settings, configManager, cache);

        lagoonMember = serverPlayer("LagoonMember", "lagoon_covenant");
        lagoonMate = serverPlayer("LagoonMate", "lagoon_covenant");
        azureMember = serverPlayer("AzureMember", "azure_hearth");
        spyAdmin = serverPlayer("SpyAdmin", null);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void alignmentChatIsDeliveredOnlyToSameAlignmentMembers() {
        assertEquals(AlignmentChatService.ChatResult.SENT,
                chatService.sendAlignmentChat(lagoonMember, "hello covenant"));

        List<String> memberMessages = drainMessages(lagoonMember);
        List<String> mateMessages = drainMessages(lagoonMate);
        List<String> azureMessages = drainMessages(azureMember);

        assertTrue(memberMessages.stream().anyMatch(msg -> msg.contains("hello covenant")));
        assertTrue(mateMessages.stream().anyMatch(msg -> msg.contains("hello covenant")),
                "same-alignment member must receive the message");
        assertTrue(mateMessages.stream().anyMatch(msg -> msg.contains("Lagoon Covenant")));
        assertTrue(mateMessages.stream().anyMatch(msg -> msg.contains("LagoonMember")));
        assertTrue(azureMessages.isEmpty(),
                "members of other alignments must not receive the message");
    }

    @Test
    void chatspyAdminReceivesAlignmentChatAfterToggling() {
        spyAdmin.setOp(true); // sunway.align.chatspy defaults to op
        assertTrue(chatService.toggleSpy(spyAdmin.getUniqueId()));

        assertEquals(AlignmentChatService.ChatResult.SENT,
                chatService.sendAlignmentChat(lagoonMember, "spy check"));

        List<String> spyMessages = drainMessages(spyAdmin);
        assertTrue(spyMessages.stream().anyMatch(msg ->
                        msg.contains("spy check") && msg.contains("[SPY]")),
                "toggled spy must receive the message with a spy tag, got: " + spyMessages);
        assertTrue(drainMessages(azureMember).isEmpty(),
                "players without alignment or spy toggle must not receive it");
    }

    @Test
    void spyWhoIsAlsoInTheAlignmentReceivesExactlyOneCopy() {
        PlayerMock spyMember = serverPlayer("SpyMember", "lagoon_covenant");
        spyMember.setOp(true); // sunway.align.chatspy defaults to op
        chatService.toggleSpy(spyMember.getUniqueId());

        chatService.sendAlignmentChat(lagoonMember, "once only");

        long copies = drainMessages(spyMember).stream()
                .filter(msg -> msg.contains("once only"))
                .count();
        assertEquals(1, copies, "alignment recipients must not get a second spy copy");
    }

    @Test
    void playersWithoutAlignmentAreAskedToJoinFirst() {
        assertEquals(AlignmentChatService.ChatResult.NO_ALIGNMENT,
                chatService.sendAlignmentChat(spyAdmin, "anyone there?"));
        assertTrue(drainMessages(lagoonMember).isEmpty());
    }

    @Test
    void alignmentChatCanBeDisabledInConfig() {
        when(settings.isAllowAlignmentChat()).thenReturn(false);
        assertEquals(AlignmentChatService.ChatResult.DISABLED,
                chatService.sendAlignmentChat(lagoonMember, "hello?"));
        assertTrue(drainMessages(lagoonMember).isEmpty());
    }

    @Test
    void blankMessagesAreRejected() {
        assertEquals(AlignmentChatService.ChatResult.EMPTY_MESSAGE,
                chatService.sendAlignmentChat(lagoonMember, "   "));
    }

    @Test
    void toggleSpyFlipsState() {
        assertTrue(chatService.toggleSpy(spyAdmin.getUniqueId()));
        assertTrue(chatService.isSpy(spyAdmin.getUniqueId()));
        assertFalse(chatService.toggleSpy(spyAdmin.getUniqueId()));
        assertFalse(chatService.isSpy(spyAdmin.getUniqueId()));
        assertEquals(0, chatService.getSpyCount());
    }

    private PlayerMock serverPlayer(String name, String alignmentId) {
        PlayerMock player = server.addPlayer(name);
        if (alignmentId != null) {
            when(cache.getOrLoad(player.getUniqueId())).thenReturn(Optional.of(new CachedMembership(
                    player.getUniqueId(), alignmentId,
                    GrandAlliance.CONCORDAT_OF_THE_DAWN.getId(),
                    Campus.SUNWAY.getId(), 0, "active", System.currentTimeMillis())));
        } else {
            when(cache.getOrLoad(player.getUniqueId())).thenReturn(Optional.empty());
        }
        return player;
    }

    private List<String> drainMessages(PlayerMock player) {
        List<String> messages = new ArrayList<>();
        String message;
        while ((message = player.nextMessage()) != null) {
            messages.add(message);
        }
        return messages;
    }
}
