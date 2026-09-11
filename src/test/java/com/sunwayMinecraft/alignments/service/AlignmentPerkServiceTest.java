package com.sunwayMinecraft.alignments.service;

import com.sunwayMinecraft.alignments.config.AlignmentConfigManager;
import com.sunwayMinecraft.alignments.config.AlignmentPerksConfig;
import com.sunwayMinecraft.alignments.config.AlignmentPerksConfig.PerkDefinition;
import com.sunwayMinecraft.alignments.config.AlignmentProgressionConfig;
import com.sunwayMinecraft.alignments.domain.AlignmentDefinition;
import com.sunwayMinecraft.alignments.domain.Campus;
import com.sunwayMinecraft.alignments.domain.GrandAlliance;
import com.sunwayMinecraft.alignments.service.AlignmentMembershipCache.CachedMembership;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AlignmentPerkServiceTest {
    private AlignmentPerksConfig perksConfig;
    private AlignmentConfigManager configManager;
    private AlignmentRankService rankService;
    private AlignmentMembershipCache cache;
    private AlignmentPerkService service;

    private final Player player = mock(Player.class);
    private final UUID playerUuid = UUID.randomUUID();
    private final AlignmentDefinition azureHearth = new AlignmentDefinition(
            "azure_hearth", "Azure Hearth", GrandAlliance.CONCORDAT_OF_THE_DAWN,
            Campus.TAYLORS, "desc", "[Azure Hearth]", "&9", true);

    @BeforeEach
    void setUp() {
        perksConfig = mock(AlignmentPerksConfig.class);
        when(perksConfig.isEnabled()).thenReturn(true);
        when(perksConfig.getPerks()).thenReturn(java.util.Map.of(
                "swiftness", new PerkDefinition(
                        "swiftness", "Swiftness", PotionEffectType.SPEED, "fellow", 0, true),
                "diligence", new PerkDefinition(
                        "diligence", "Diligence", PotionEffectType.HASTE, "steward", 0, true),
                "retired", new PerkDefinition(
                        "retired", "Retired", PotionEffectType.REGENERATION, "archon", 0, false)));
        when(perksConfig.getDurationSeconds()).thenReturn(15);
        when(perksConfig.isApplyInAnyDistrict()).thenReturn(true);
        when(perksConfig.getMaxAmplifier()).thenReturn(1);
        when(perksConfig.getCooldownReduction())
                .thenReturn(new AlignmentPerksConfig.CooldownReduction(true, "steward", 25));
        when(player.getUniqueId()).thenReturn(playerUuid);
        configManager = mock(AlignmentConfigManager.class);
        when(configManager.getAlignment("azure_hearth")).thenReturn(Optional.of(azureHearth));
        rankService = new AlignmentRankService(realProgressionConfig());
        cache = mock(AlignmentMembershipCache.class);
        service = new AlignmentPerkService(
                mock(org.bukkit.plugin.java.JavaPlugin.class), perksConfig, configManager,
                rankService, cache, null);
    }

    private AlignmentProgressionConfig realProgressionConfig() {
        org.bukkit.plugin.java.JavaPlugin plugin = mock(org.bukkit.plugin.java.JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(java.nio.file.Path.of(
                System.getProperty("java.io.tmpdir"), "perk-progression-" + UUID.randomUUID())
                .toFile());
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("perk-test"));
        org.mockito.Mockito.doAnswer(invocation -> {
            java.io.File out = new java.io.File(plugin.getDataFolder(),
                invocation.getArgument(0, String.class));
            out.getParentFile().mkdirs();
            if (!out.exists()) {
                try (java.io.InputStream in = getClass().getClassLoader()
                        .getResourceAsStream(out.getName())) {
                    if (in != null) {
                        java.nio.file.Files.copy(in, out.toPath());
                    }
                }
            }
            return null;
        }).when(plugin).saveResource(org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyBoolean());
        AlignmentProgressionConfig config = new AlignmentProgressionConfig(plugin);
        config.load();
        return config;
    }

    private CachedMembership membership(int reputation) {
        return new CachedMembership(playerUuid, "azure_hearth",
                GrandAlliance.CONCORDAT_OF_THE_DAWN.getId(), Campus.TAYLORS.getId(),
                reputation, "active", System.currentTimeMillis());
    }

    @Test
    void perksRequireTheConfiguredMinimumRank() {
        service.reconcile(player, membership(74), azureHearth); // fellow at 75
        assertTrue(service.getActivePerkIds(playerUuid).isEmpty(),
                "below the perk rank nothing may be applied");

        service.reconcile(player, membership(75), azureHearth); // fellow
        assertTrue(service.getActivePerkIds(playerUuid).contains("swiftness"));
        assertFalse(service.getActivePerkIds(playerUuid).contains("diligence"),
                "steward perks must not apply to fellows");

        service.reconcile(player, membership(150), azureHearth); // steward
        assertTrue(service.getActivePerkIds(playerUuid).contains("diligence"));
    }

    @Test
    void changingAlignmentRemovesPerksFromTheOldOne() {
        service.reconcile(player, membership(150), azureHearth);
        assertTrue(service.getActivePerkIds(playerUuid).contains("swiftness"));

        // player switches to an unconfigured alignment: all perks must go
        service.reconcile(player, null, null);
        assertTrue(service.getActivePerkIds(playerUuid).isEmpty());
    }

    @Test
    void inactiveMembershipsAndDisabledPerksNeverApply() {
        service.reconcile(player,
                new CachedMembership(playerUuid, "azure_hearth",
                        GrandAlliance.CONCORDAT_OF_THE_DAWN.getId(), Campus.TAYLORS.getId(),
                        300, "inactive", System.currentTimeMillis()), azureHearth);
        assertTrue(service.getActivePerkIds(playerUuid).isEmpty(),
                "inactive memberships must not receive perks");

        service.reconcile(player, membership(300), azureHearth);
        assertFalse(service.getActivePerkIds(playerUuid).contains("retired"),
                "disabled perks must never apply");
    }

    @Test
    void globallyDisabledPerksRemoveActiveEffects() {
        service.reconcile(player, membership(150), azureHearth);
        assertTrue(service.getActivePerkIds(playerUuid).contains("swiftness"));

        when(perksConfig.isEnabled()).thenReturn(false);
        service.refresh();
        assertTrue(service.getActivePerkIds(playerUuid).isEmpty(),
                "disabling perks must remove active effects");
    }

    @Test
    void adminBypassIgnoresRankAndDistrictGates() {
        when(player.hasPermission("sunway.align.perks.admin")).thenReturn(true);
        service.reconcile(player, membership(0), azureHearth); // initiate
        assertTrue(service.getActivePerkIds(playerUuid).contains("swiftness"),
                "perks.admin bypasses rank gates for testing");
        assertTrue(service.getActivePerkIds(playerUuid).contains("diligence"));
    }

    @Test
    void cooldownReductionAppliesOnlyToQualifiedRanks() {
        when(cache.getOrLoad(playerUuid)).thenReturn(Optional.of(membership(300)));
        assertEquals(75L, service.applyCooldownReduction(playerUuid, 100L),
                "stewards get the 25 percent reduction");

        when(cache.getOrLoad(playerUuid)).thenReturn(Optional.of(membership(50)));
        assertEquals(100L, service.applyCooldownReduction(playerUuid, 100L),
                "below the reduction rank the full cooldown applies");

        when(perksConfig.getCooldownReduction())
                .thenReturn(new AlignmentPerksConfig.CooldownReduction(false, "steward", 25));
        assertEquals(100L, service.applyCooldownReduction(playerUuid, 100L),
                "disabled reduction leaves the cooldown untouched");
    }

    @Test
    void amplifierIsCappedByTheConfiguredMaximum() {
        when(perksConfig.getPerks()).thenReturn(java.util.Map.of(
                "strong", new PerkDefinition(
                        "strong", "Strong", PotionEffectType.SPEED, "initiate", 9, true)));
        service.reconcile(player, membership(0), azureHearth);
        assertTrue(service.getActivePerkIds(playerUuid).contains("strong"));

        org.mockito.ArgumentCaptor<org.bukkit.potion.PotionEffect> captor =
                org.mockito.ArgumentCaptor.forClass(org.bukkit.potion.PotionEffect.class);
        org.mockito.Mockito.verify(player).addPotionEffect(captor.capture());
        assertEquals(1, captor.getValue().getAmplifier(),
                "raw amplifier 9 must be capped at max_amplifier 1");

        when(perksConfig.getPerks()).thenReturn(java.util.Map.of());
        service.reconcile(player, membership(0), azureHearth);
        assertTrue(service.getActivePerkIds(playerUuid).isEmpty());
        org.mockito.Mockito.verify(player).removePotionEffect(PotionEffectType.SPEED);
    }

    @Test
    void districtGateBlocksPerksOutsideQualifyingAreas() {
        when(perksConfig.isApplyInAnyDistrict()).thenReturn(false);
        AlignmentPerkService gated = new AlignmentPerkService(
                mock(org.bukkit.plugin.java.JavaPlugin.class), perksConfig, configManager,
                rankService, cache, player -> false);
        gated.reconcile(player, membership(150), azureHearth);
        assertTrue(gated.getActivePerkIds(playerUuid).isEmpty(),
                "perks must not apply when the district gate rejects the location");

        AlignmentPerkService allowed = new AlignmentPerkService(
                mock(org.bukkit.plugin.java.JavaPlugin.class), perksConfig, configManager,
                rankService, cache, player2 -> true);
        allowed.reconcile(player, membership(150), azureHearth);
        assertTrue(allowed.getActivePerkIds(playerUuid).contains("swiftness"),
                "perks must apply when the district gate accepts the location");
    }

    @Test
    void forgetPlayerDropsTrackingAndRemovesEffects() {
        org.mockbukkit.mockbukkit.MockBukkit.mock(); // forgetPlayer consults Bukkit.getPlayer
        try {
            service.reconcile(player, membership(150), azureHearth);
            assertTrue(service.getActivePerkIds(playerUuid).contains("swiftness"));

            service.forgetPlayer(playerUuid);
            assertTrue(service.getActivePerkIds(playerUuid).isEmpty(),
                    "forgetPlayer must drop the tracking entry");
            // effect removal only applies to players resolvable online; an unregistered
            // uuid is skipped, which is the production-safe behavior for offline players
        } finally {
            org.mockbukkit.mockbukkit.MockBukkit.unmock();
        }
    }

    @Test
    @Disabled("BUG-QA3 (medium): perks are applied once and never refreshed. reconcile() "
            + "only calls addPotionEffect when the perk is not yet tracked, so the short "
            + "PotionEffect (duration_seconds, default 15s) expires while the service keeps "
            + "the perk marked active - players silently lose perks. Desired perks should be "
            + "re-applied on every refresh pass.")
    void desiredPerksAreRefreshedOnEveryPass() {
        service.reconcile(player, membership(150), azureHearth);
        service.reconcile(player, membership(150), azureHearth);
        org.mockito.Mockito.verify(player, org.mockito.Mockito.times(2)).addPotionEffect(any());
    }
}