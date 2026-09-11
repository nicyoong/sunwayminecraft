package com.sunwayMinecraft.alignments.listener;

import com.sunwayMinecraft.alignments.config.AlignmentConfigManager;
import com.sunwayMinecraft.alignments.domain.AlignmentDefinition;
import com.sunwayMinecraft.alignments.domain.Campus;
import com.sunwayMinecraft.alignments.domain.GrandAlliance;
import com.sunwayMinecraft.alignments.domain.AlignmentMembership;
import com.sunwayMinecraft.alignments.persistence.AlignmentRepository;
import com.sunwayMinecraft.alignments.service.AlignmentMembershipCache;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AlignmentPlayerListenerTest {
    private ServerMock server;
    private AlignmentMembershipCache cache;
    private AlignmentPlayerListener listener;

    private final AlignmentDefinition azureHearth = new AlignmentDefinition(
            "azure_hearth", "Azure Hearth", GrandAlliance.CONCORDAT_OF_THE_DAWN,
            Campus.TAYLORS, "desc", "[Azure Hearth]", "&9", true);

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        AlignmentConfigManager configManager = mock(AlignmentConfigManager.class);
        AlignmentRepository repository = mock(AlignmentRepository.class);
        when(configManager.getAlignment("azure_hearth")).thenReturn(Optional.of(azureHearth));
        when(repository.findByUuid(any(UUID.class))).thenAnswer(invocation ->
                Optional.of(AlignmentMembership.newMembership(
                        invocation.getArgument(0, UUID.class), "azure_hearth", 1000L)));
        cache = new AlignmentMembershipCache(configManager, repository);
        listener = new AlignmentPlayerListener(cache);
        JavaPlugin plugin = MockBukkit.createMockPlugin();
        listener.register(plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void playerJoinLoadsMembershipIntoTheCache() {
        PlayerMock player = server.addPlayer();

        assertTrue(cache.get(player.getUniqueId()).isPresent(),
                "joining players must have their membership loaded into the cache");
        assertTrue(cache.get(player.getUniqueId()).get().alignmentId().equals("azure_hearth"));
    }

    @Test
    void playerQuitUnloadsMembershipFromTheCache() {
        PlayerMock player = server.addPlayer();
        assertTrue(cache.get(player.getUniqueId()).isPresent());

        player.disconnect();

        assertFalse(cache.get(player.getUniqueId()).isPresent(),
                "quitting players must have their cache entry removed");
    }
}
