package com.sunwayMinecraft.alignments.listener;

import com.sunwayMinecraft.alignments.config.AlignmentConfigManager;
import com.sunwayMinecraft.alignments.config.AlignmentProgressionConfig;
import com.sunwayMinecraft.alignments.config.AlignmentSettingsConfig;
import com.sunwayMinecraft.alignments.domain.AlignmentDefinition;
import com.sunwayMinecraft.alignments.domain.Campus;
import com.sunwayMinecraft.alignments.domain.GrandAlliance;
import com.sunwayMinecraft.alignments.domain.GrandAllianceDefinition;
import com.sunwayMinecraft.alignments.service.AlignmentMembershipCache;
import com.sunwayMinecraft.alignments.service.AlignmentMembershipCache.CachedMembership;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AlignmentChatListenerTest {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private AlignmentSettingsConfig settings;
    private AlignmentConfigManager configManager;
    private AlignmentMembershipCache cache;
    private com.sunwayMinecraft.alignments.config.AlignmentProgressionConfig progressionConfig;
    private AlignmentChatListener listener;
    private org.mockbukkit.mockbukkit.ServerMock server;

    private PlayerMock player;
    private final AlignmentDefinition azureHearth = new AlignmentDefinition(
            "azure_hearth", "Azure Hearth", GrandAlliance.CONCORDAT_OF_THE_DAWN,
            Campus.TAYLORS, "desc", "[Azure Hearth]", "&9", true);
    private final AlignmentDefinition retiredOrder = new AlignmentDefinition(
            "retired_order", "Retired Order", GrandAlliance.IRONCLAD_SYNDICATE,
            Campus.MONASH, "desc", "[Retired Order]", "&8", false);

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        settings = mock(AlignmentSettingsConfig.class);
        configManager = mock(AlignmentConfigManager.class);
        cache = mock(AlignmentMembershipCache.class);
        when(settings.isAllowGlobalChatPrefix()).thenReturn(true);
        when(settings.getChatPrefixFormat()).thenReturn("&7[{grand_alliance_short} | {alignment}&7] &r");
        when(settings.getDisabledPrefixMode()).thenReturn(AlignmentSettingsConfig.DisabledPrefixMode.HIDE);
        when(configManager.getAlignment("azure_hearth")).thenReturn(Optional.of(azureHearth));
        when(configManager.getAlignment("retired_order")).thenReturn(Optional.of(retiredOrder));
        when(configManager.getAllianceDefinition(GrandAlliance.CONCORDAT_OF_THE_DAWN))
                .thenReturn(Optional.of(new GrandAllianceDefinition(
                        "concordat_of_the_dawn", "Concordat of the Dawn", "desc", "&b")));
        progressionConfig = mock(com.sunwayMinecraft.alignments.config.AlignmentProgressionConfig.class);
        listener = new AlignmentChatListener(
            settings, configManager, cache,
            mock(com.sunwayMinecraft.alignments.service.AlignmentRankService.class),
            progressionConfig);
        player = server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void alignedPlayersGetAnAllianceAndAlignmentPrefix() {
        cacheMembership("azure_hearth", "active");

        String prefix = LEGACY.serialize(listener.buildPrefix(player));

        assertTrue(prefix.contains("Concordat"), "short alliance name expected, got: " + prefix);
        assertTrue(prefix.contains("Azure Hearth"), "alignment name expected, got: " + prefix);
        assertTrue(prefix.startsWith("§7[") || prefix.contains("§7"),
                "prefix should use the configured colour, got: " + prefix);
    }

    @Test
    void unalignedPlayersGetNoPrefix() {
        when(cache.getOrLoad(player.getUniqueId())).thenReturn(Optional.empty());
        assertNull(listener.buildPrefix(player));
    }

    @Test
    void globalPrefixCanBeDisabledEntirely() {
        when(settings.isAllowGlobalChatPrefix()).thenReturn(false);
        cacheMembership("azure_hearth", "active");
        assertNull(listener.buildPrefix(player));
    }

    @Test
    void inactiveMembershipsGetNoPrefix() {
        cacheMembership("azure_hearth", "inactive");
        assertNull(listener.buildPrefix(player));
    }

    @Test
    void disabledAlignmentsHideOrNeutraliseThePrefixPerConfig() {
        cacheMembership("retired_order", "active");
        assertNull(listener.buildPrefix(player), "HIDE mode must not add a prefix");

        when(settings.getDisabledPrefixMode()).thenReturn(AlignmentSettingsConfig.DisabledPrefixMode.NEUTRAL);
        String prefix = LEGACY.serialize(listener.buildPrefix(player));
        assertTrue(prefix.contains("Unaligned"), "NEUTRAL mode expected [Unaligned], got: " + prefix);
    }

    @Test
    void vanishedAlignmentsFollowTheSamePolicyAsDisabledOnes() {
        when(cache.getOrLoad(player.getUniqueId())).thenReturn(Optional.of(new CachedMembership(
                player.getUniqueId(), "vanished_order",
                GrandAlliance.IRONCLAD_SYNDICATE.getId(), Campus.MONASH.getId(),
                0, "active", System.currentTimeMillis())));
        when(configManager.getAlignment("vanished_order")).thenReturn(Optional.empty());

        assertNull(listener.buildPrefix(player));
    }

    private void cacheMembership(String alignmentId, String status) {
        when(cache.getOrLoad(player.getUniqueId())).thenReturn(Optional.of(new CachedMembership(
                player.getUniqueId(), alignmentId,
                GrandAlliance.CONCORDAT_OF_THE_DAWN.getId(), Campus.TAYLORS.getId(),
                0, status, System.currentTimeMillis())));
    }

    @Test
    void rankSuffixIsAppendedWhenTheRankHasOneAndIsEnabled() {
        when(settings.isAllowChatRankSuffix()).thenReturn(true);
        when(settings.getChatRankSuffixFormat()).thenReturn("&7[{rank_suffix}&7] &r");
        when(progressionConfig.getRank("concordat_of_the_dawn", "steward"))
                .thenReturn(Optional.of(new AlignmentProgressionConfig.RankDefinition(
                        "steward", "Steward", 150, "Steward", "desc", true)));
        cacheMembershipWithRank("azure_hearth", "active", "steward");

        String prefix = LEGACY.serialize(listener.buildPrefix(player));
        assertTrue(prefix.contains("Steward"), "rank suffix expected, got: " + prefix);
        assertTrue(prefix.contains("Azure Hearth"), "alignment prefix expected, got: " + prefix);
    }

    @Test
    void ranksWithoutASuffixAddNothingWhenOnlyTheSuffixIsEnabled() {
        when(settings.isAllowGlobalChatPrefix()).thenReturn(false);
        when(settings.isAllowChatRankSuffix()).thenReturn(true);
        when(settings.getChatRankSuffixFormat()).thenReturn("&7[{rank_suffix}&7] &r");
        cacheMembershipWithRank("azure_hearth", "active", "initiate");

        assertNull(listener.buildPrefix(player),
                "empty chat_suffix must not render a suffix-only prefix");
    }

    @Test
    void bothPrefixAndSuffixDisabledLeavesChatUntouched() {
        when(settings.isAllowGlobalChatPrefix()).thenReturn(false);
        when(settings.isAllowChatRankSuffix()).thenReturn(false);
        cacheMembership("azure_hearth", "active");
        assertNull(listener.buildPrefix(player));
    }

    private void cacheMembershipWithRank(String alignmentId, String status, String rankId) {
        when(cache.getOrLoad(player.getUniqueId())).thenReturn(Optional.of(new CachedMembership(
                player.getUniqueId(), alignmentId,
                GrandAlliance.CONCORDAT_OF_THE_DAWN.getId(), Campus.TAYLORS.getId(),
                150, status, System.currentTimeMillis(), rankId)));
    }
}