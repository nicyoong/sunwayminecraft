package com.sunwayMinecraft.districts.listener;

import com.sunwayMinecraft.districts.config.DistrictSettingsConfig;
import com.sunwayMinecraft.districts.config.DistrictSettingsConfig.InteractionFlags;
import com.sunwayMinecraft.districts.domain.DistrictDefinition;
import com.sunwayMinecraft.districts.domain.DistrictOwnership;
import com.sunwayMinecraft.districts.domain.DistrictType;
import com.sunwayMinecraft.districts.region.DistrictLocationResolver;
import com.sunwayMinecraft.districts.region.DistrictShape;
import com.sunwayMinecraft.districts.region.Region3i;
import com.sunwayMinecraft.districts.service.DistrictAlignmentService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Build, interaction and sanctuary enforcement of the district protection
 * listener, exercised with mocked world blocks.
 */
class DistrictProtectionListenerTest {
    private DistrictLocationResolver resolver;
    private DistrictSettingsConfig settings;
    private DistrictProtectionListener listener;

    private final Player player = mock(Player.class);
    private final UUID playerUuid = UUID.randomUUID();

    private Block block;
    private Location blockLocation;

    @BeforeEach
    void setUp() {
        resolver = mock(DistrictLocationResolver.class);
        settings = mock(DistrictSettingsConfig.class);
        when(settings.isEnforceBuildRules()).thenReturn(true);
        when(settings.isEnforceInteractionRules()).thenReturn(true);
        when(settings.isArchivedReadOnly()).thenReturn(true);
        when(settings.isSanctuaryNoBuild()).thenReturn(true);
        when(settings.isSanctuaryNoPvp()).thenReturn(true);
        when(settings.interactionFlagsFor(any())).thenReturn(InteractionFlags.ALL);

        when(player.getUniqueId()).thenReturn(playerUuid);
        when(player.hasPermission("sunway.district.admin.bypass")).thenReturn(false);

        DistrictAlignmentService service = new DistrictAlignmentService(
                mock(com.sunwayMinecraft.districts.config.DistrictsConfigManager.class),
                uuid -> Optional.empty()); // every player is unaligned in this fixture
        listener = new DistrictProtectionListener(
                mock(org.bukkit.plugin.java.JavaPlugin.class), resolver, service, settings,
                () -> null);

        org.bukkit.World world = mock(org.bukkit.World.class);
        when(world.getName()).thenReturn("world");
        block = mock(Block.class);
        blockLocation = new Location(world, 10, 64, 10);
        when(block.getLocation()).thenReturn(blockLocation);
        when(block.getType()).thenReturn(Material.CHEST);
    }

    private DistrictDefinition district(DistrictType type, DistrictOwnership ownership) {
        return new DistrictDefinition("test", "Test", null, "world",
                DistrictShape.cuboid(new Region3i("world", 0, 0, 0, 9, 9, 9)), true, type, 1, "summary",
                java.util.List.of(), true, 50, false, false, null, false, false, ownership);
    }

    private PlayerInteractEvent rightClick() {
        // a null ItemStack is valid: the listener only reads the clicked block
        return new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK,
                null, block, org.bukkit.block.BlockFace.UP, EquipmentSlot.HAND);
    }

    @Test
    void buildIsDeniedInArchivedDistrictsWithTheReadOnlyMessage() {
        when(resolver.getDistrictAt(blockLocation))
                .thenReturn(district(DistrictType.ARCHIVED, DistrictOwnership.neutral()));

        BlockBreakEvent event = new BlockBreakEvent(block, player);
        listener.onBlockBreak(event);

        assertTrue(event.isCancelled(), "archived districts must deny block breaking");
        org.mockito.Mockito.verify(player).sendMessage(org.mockito.ArgumentMatchers.argThat(
                (String m) -> m != null && m.contains("read-only")));
    }

    @Test
    void buildIsDeniedForPlayersWithoutAccess() {
        // unaligned players are denied on whitelist districts
        when(resolver.getDistrictAt(blockLocation))
                .thenReturn(district(DistrictType.CAMPUS,
                        new DistrictOwnership("taylors", null, null,
                                java.util.List.of("azure_hearth"), java.util.List.of(),
                                null, false, false)));

        BlockPlaceEvent event = new BlockPlaceEvent(block,
                mock(org.bukkit.block.BlockState.class), block, null, player, false);
        listener.onBlockPlace(event);

        assertTrue(event.isCancelled(), "access-denied players must not build");
    }

    @Test
    void adminBypassBuildsEverywhere() {
        when(player.hasPermission("sunway.district.admin.bypass")).thenReturn(true);
        when(resolver.getDistrictAt(blockLocation))
                .thenReturn(district(DistrictType.ARCHIVED, DistrictOwnership.neutral()));

        BlockPlaceEvent event = new BlockPlaceEvent(block,
                mock(org.bukkit.block.BlockState.class), block, null, player, false);
        listener.onBlockPlace(event);

        assertFalse(event.isCancelled(), "the admin bypass must unlock archived districts");
    }

    @Test
    void enforcingOffLeavesBuildingUntouched() {
        when(settings.isEnforceBuildRules()).thenReturn(false);
        when(resolver.getDistrictAt(blockLocation))
                .thenReturn(district(DistrictType.ARCHIVED, DistrictOwnership.neutral()));

        BlockBreakEvent event = new BlockBreakEvent(block, player);
        listener.onBlockBreak(event);

        assertFalse(event.isCancelled(), "enforce_build_rules=false must allow building");
    }

    @Test
    @Disabled("BUG-DIST4 (medium): access-denied players are only denied when the per-type "
            + "interaction flag also denies the category. With the shipped all-true flags an "
            + "alignment-denied player can still open chests in a district they are barred "
            + "from. Access denial must deny every interaction category.")
    void interactionIsDeniedForPlayersTheAccessRuleDenies() {
        when(resolver.getDistrictAt(blockLocation))
                .thenReturn(district(DistrictType.CAMPUS,
                        new DistrictOwnership("taylors", null, null,
                                java.util.List.of("azure_hearth"), java.util.List.of(),
                                null, false, false)));

        PlayerInteractEvent event = rightClick();
        listener.onPlayerInteract(event);

        assertTrue(event.isCancelled(), "access-denied players must not interact");
    }

    @Test
    void neutralDistrictsAllowInteraction() {
        when(resolver.getDistrictAt(blockLocation))
                .thenReturn(district(DistrictType.CIVIC, DistrictOwnership.neutral()));

        PlayerInteractEvent event = rightClick();
        listener.onPlayerInteract(event);

        assertFalse(event.isCancelled(), "neutral districts must not block interaction");
    }

    @Test
    @Disabled("BUG-DIST3 (medium): onPlayerInteract returns early whenever the player "
            + "passes the access check, so the per-type interaction flags (including the "
            + "ARCHIVED allow_containers=false defaults) are never evaluated for players "
            + "who are allowed to access the district. Archived read-only interaction "
            + "protection is therefore dead code; the flag check must also run for "
            + "access-allowed players.")
    void archivedInteractionFlagsApplyEvenToAccessAllowedPlayers() {
        when(resolver.getDistrictAt(blockLocation))
                .thenReturn(district(DistrictType.ARCHIVED, DistrictOwnership.neutral()));
        when(settings.interactionFlagsFor(DistrictType.ARCHIVED))
                .thenReturn(InteractionFlags.NONE);

        PlayerInteractEvent event = rightClick();
        listener.onPlayerInteract(event);

        assertTrue(event.isCancelled(),
                "the ARCHIVED interaction flags must deny container access");
    }
}
