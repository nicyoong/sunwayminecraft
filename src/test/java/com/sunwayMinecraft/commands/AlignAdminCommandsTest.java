package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.alignments.config.AlignmentConfigManager;
import com.sunwayMinecraft.alignments.config.AlignmentSettingsConfig;
import com.sunwayMinecraft.alignments.domain.AlignmentDefinition;
import com.sunwayMinecraft.alignments.domain.AlignmentMembership;
import com.sunwayMinecraft.alignments.domain.Campus;
import com.sunwayMinecraft.alignments.domain.GrandAlliance;
import com.sunwayMinecraft.alignments.service.AlignmentMembershipCache;
import com.sunwayMinecraft.alignments.service.AlignmentMembershipCache.CachedMembership;
import com.sunwayMinecraft.alignments.service.AlignmentResult;
import com.sunwayMinecraft.alignments.service.AlignmentService;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlignAdminCommandsTest {
    private ServerMock server;
    private PlayerMock admin;
    private AlignmentService service;
    private AlignmentConfigManager configManager;
    private AlignmentSettingsConfig settings;
    private AlignmentMembershipCache cache;
    private AlignAdminCommands adminCommands;

    private final AlignmentDefinition azureHearth = new AlignmentDefinition(
            "azure_hearth", "Azure Hearth", GrandAlliance.CONCORDAT_OF_THE_DAWN,
            Campus.TAYLORS, "desc", "[Azure Hearth]", "&9", true);

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        admin = server.addPlayer("Admin");
        admin.setOp(true);
        service = mock(AlignmentService.class);
        configManager = mock(AlignmentConfigManager.class);
        settings = mock(AlignmentSettingsConfig.class);
        cache = mock(AlignmentMembershipCache.class);
        when(configManager.getAlignment("azure_hearth")).thenReturn(Optional.of(azureHearth));
        adminCommands = new AlignAdminCommands(service, configManager, settings, cache);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void setForcesTheTargetIntoTheRequestedAlignment() {
        PlayerMock target = server.addPlayer("Target");
        when(service.adminSet(target.getUniqueId(), "azure_hearth"))
                .thenReturn(AlignmentResult.JOINED);

        assertTrue(adminCommandsCall("set", "Target", "azure", "hearth"));

        verify(service).adminSet(target.getUniqueId(), "azure_hearth");
        List<String> messages = drainMessages(admin);
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("Set §fTarget")), messages.toString());
        assertTrue(drainMessages(target).stream()
                .anyMatch(msg -> msg.contains("set by an admin")));
    }

    @Test
    void setReportsUnknownPlayersAlignmentsAndRequiresAdmin() {
        PlayerMock target = server.addPlayer("Target");

        CommandSender nonAdmin = mock(CommandSender.class);
        when(nonAdmin.hasPermission("sunway.align.admin")).thenReturn(false);
        adminCommands.handleSet(nonAdmin, new String[]{"set", "x", "y"});
        verify(nonAdmin).sendMessage(org.mockito.ArgumentMatchers.argThat(
                (String message) -> message != null && message.contains("permission")));
        verify(service, never()).adminSet(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

        assertTrue(adminCommandsCall("set", "Ghost", "azure_hearth"));
        assertTrue(drainMessages(admin).stream().anyMatch(msg -> msg.contains("Player not found")));

        when(service.adminSet(target.getUniqueId(), "nope"))
                .thenReturn(AlignmentResult.DISABLED);
        assertTrue(adminCommandsCall("set", "Target", "nope"));
        assertTrue(drainMessages(admin).stream().anyMatch(msg -> msg.contains("disabled")));
    }

    @Test
    void clearRemovesMembershipAndReportsUnalignedTargets() {
        PlayerMock target = server.addPlayer("Target");
        when(service.adminClear(target.getUniqueId())).thenReturn(AlignmentResult.LEFT);

        assertTrue(adminCommandsCall("clear", "Target"));

        verify(service).adminClear(target.getUniqueId());
        assertTrue(drainMessages(admin).stream().anyMatch(msg -> msg.contains("Cleared §fTarget")),
                drainMessages(admin).toString());

        when(service.adminClear(target.getUniqueId())).thenReturn(AlignmentResult.NOT_ALIGNED);
        assertTrue(adminCommandsCall("clear", "Target"));
        assertTrue(drainMessages(admin).stream().anyMatch(msg -> msg.contains("not aligned")));
    }

    @Test
    void infoShowsReputationStatusCacheStateAndJoinDate() {
        PlayerMock target = server.addPlayer("Target");
        Optional<CachedMembership> cached = Optional.of(new CachedMembership(
                target.getUniqueId(), "azure_hearth",
                GrandAlliance.CONCORDAT_OF_THE_DAWN.getId(), Campus.TAYLORS.getId(),
                25, "active", System.currentTimeMillis()));
        when(cache.getOrLoad(target.getUniqueId())).thenReturn(cached);
        when(cache.get(target.getUniqueId())).thenReturn(cached);
        when(service.getMembership(target.getUniqueId())).thenReturn(Optional.of(
                AlignmentMembership.newMembership(target.getUniqueId(), "azure_hearth", 1000L)));

        assertTrue(adminCommandsCall("info", "Target"));

        List<String> messages = drainMessages(admin);
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("Reputation: §f25")), messages.toString());
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("Status: §factive")), messages.toString());
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("cached")), messages.toString());
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("Joined:")), messages.toString());
    }

    @Test
    void infoReportsUnalignedPlayersWithTheirCacheState() {
        PlayerMock target = server.addPlayer("Target");
        when(cache.getOrLoad(target.getUniqueId())).thenReturn(Optional.empty());

        assertTrue(adminCommandsCall("info", "Target"));

        List<String> messages = drainMessages(admin);
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("Unaligned")), messages.toString());
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("database only")), messages.toString());
    }

    @Test
    void reloadReloadsBothConfigsAndRevalidatesTheCache() {
        assertTrue(adminCommandsCall("reload"));

        verify(configManager).load();
        verify(settings).load();
        verify(cache).revalidate(settings);
        assertTrue(drainMessages(admin).stream().anyMatch(msg -> msg.contains("reloaded")));
    }

    private boolean adminCommandsCall(String... args) {
        switch (args[0]) {
            case "set" -> adminCommands.handleSet(admin, args);
            case "clear" -> adminCommands.handleClear(admin, args);
            case "info" -> adminCommands.handleInfo(admin, args);
            case "reload" -> adminCommands.handleReload(admin);
            default -> throw new IllegalArgumentException(args[0]);
        }
        return true;
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
