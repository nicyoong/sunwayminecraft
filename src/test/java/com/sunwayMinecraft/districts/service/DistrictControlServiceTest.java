package com.sunwayMinecraft.districts.service;

import com.sunwayMinecraft.districts.config.DistrictControlSettingsConfig;
import com.sunwayMinecraft.districts.config.DistrictsConfigManager;
import com.sunwayMinecraft.districts.domain.DistrictOwnership;
import com.sunwayMinecraft.districts.event.DistrictControlChangeEvent;
import com.sunwayMinecraft.districts.persistence.DistrictControlRepository;
import com.sunwayMinecraft.districts.persistence.DistrictControlRepository.ControlStateRecord;
import com.sunwayMinecraft.districts.region.DistrictLocationResolver;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Presence counting, contest pauses, decay, capture, persistence merge,
 * admin set and event firing for the district control service.
 */
class DistrictControlServiceTest {
    private static final String DISTRICTS_YAML = """
            districts:
              contest_zone:
                display-name: "Contest Zone"
                district-type: CAMPUS
                shape: point_radius
                center: { x: 10, y: 0, z: 10, radius: 40 }
                contest_enabled: true
                points_required_to_capture: 10
                world: "world"
                public-summary: "test"
                region: { min: {x: 0, y: 0, z: 0}, max: {x: 20, y: 20, z: 20} }
            """;

    private static final String ALIGNMENTS_YAML = """
            alignments:
              azure_hearth:
                display_name: "Azure Hearth"
                grand_alliance: concordat_of_the_dawn
              spirewrights:
                display_name: "Spirewrights"
                grand_alliance: ironclad_syndicate
            """;

    private ServerMock server;
    private JavaPlugin plugin;
    private World world;
    private Path dataDirectory;
    private DistrictsConfigManager configManager;
    private DistrictControlSettingsConfig controlSettings;
    private DistrictControlRepository repository;
    private DistrictControlService service;
    private final List<DistrictControlChangeEvent> events = new CopyOnWriteArrayList<>();

    private final Map<String, String> alignmentByPlayerName = new HashMap<>(Map.of(
            "AzurePlayer", "azure_hearth",
            "SpirePlayer", "spirewrights"));

    @BeforeEach
    void setUp() throws Exception {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        dataDirectory = Files.createTempDirectory("district-control-test");
        Files.writeString(dataDirectory.resolve("districts.yml"), DISTRICTS_YAML);
        Files.writeString(dataDirectory.resolve("alignments.yml"), ALIGNMENTS_YAML);

        plugin = pluginMock();
        configManager = new DistrictsConfigManager(plugin, id -> true);
        configManager.reload();
        controlSettings = new DistrictControlSettingsConfig(plugin);
        controlSettings.load();
        repository = new DistrictControlRepository(plugin);

        service = newControlService();
        service.load();

        server.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onChange(DistrictControlChangeEvent event) {
                events.add(event);
            }
        }, MockBukkit.createMockPlugin());
        System.out.println("DEBUG registered listeners for control change: "
                + DistrictControlChangeEvent.getHandlerList().getRegisteredListeners().length);
    }

    @AfterEach
    void tearDown() {
        repository.close();
        MockBukkit.unmock();
    }

    private JavaPlugin pluginMock() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(dataDirectory.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("DistrictControlServiceTest"));
        org.mockito.Mockito.doAnswer(invocation -> {
            String name = invocation.getArgument(0, String.class);
            java.io.File out = new java.io.File(dataDirectory.toFile(), name);
            if (!out.exists()) {
                try (java.io.InputStream in = getClass().getClassLoader().getResourceAsStream(name)) {
                    if (in != null) {
                        Files.copy(in, out.toPath());
                    }
                }
            }
            return null;
        }).when(plugin).saveResource(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyBoolean());
        return plugin;
    }

    private PlayerMock playerInZone(String name) {
        PlayerMock player = server.addPlayer(name);
        player.teleport(new Location(world, 10, 0, 10));
        return player;
    }

    private void tick(int times) {
        for (int i = 0; i < times; i++) {
            service.tick();
        }
    }

    /** Builds the service under test, resolving alignments by player name. */
    private DistrictControlService newControlService() {
        return new DistrictControlService(plugin, configManager,
                new DistrictLocationResolver(configManager), controlSettings, repository,
                uuid -> {
                    Player player = server.getPlayer(uuid);
                    return player == null ? Optional.empty()
                            : Optional.ofNullable(alignmentByPlayerName.get(player.getName()));
                },
                () -> null, null, () -> null);
    }

    @Test
    void presenceCountsTowardTheLeadingAlignmentProgress() {
        playerInZone("AzurePlayer");

        service.tick();

        var state = service.getControlState("contest_zone").get();
        assertEquals(DistrictControlService.ContestState.CAPTURING, state.state);
        assertEquals("azure_hearth", state.leadingAlignmentId);
        assertEquals(2.0, state.progress, 0.001,
                "one azure player contributes points_per_player_per_tick per tick");
    }

    @Test
    void multipleAlignmentsMarkContestedAndPauseProgress() {
        playerInZone("AzurePlayer");
        playerInZone("SpirePlayer");

        service.tick();
        service.tick();

        var state = service.getControlState("contest_zone").get();
        assertEquals(DistrictControlService.ContestState.CONTESTED, state.state);
        assertEquals(0.0, state.progress, 0.001,
                "a multi-alignment contest pauses all progress");
    }

    @Test
    void uncontestedProgressDecaysAndResetsTheDistrictToNeutral() {
        // capture first: 5 ticks at 2 points = 10 = the capture threshold
        playerInZone("AzurePlayer");
        tick(5);
        assertEquals("azure_hearth", service.getControllerAlignment("contest_zone"));
        assertEquals(DistrictControlService.ContestState.COOLING_DOWN,
                service.getControlState("contest_zone").get().state);

        // everyone leaves: the captured grip decays and the district falls to neutral
        Player azure = server.getPlayer("AzurePlayer");
        if (azure != null) {
            azure.teleport(new Location(world, 500, 0, 500));
        }
        tick(5);

        var state = service.getControlState("contest_zone").get();
        assertEquals(0.0, state.progress, 0.001);
        assertNull(state.controllerAlignmentId,
                "allow_neutral_reset must drop the controller once fully decayed");
        assertTrue(events.stream().anyMatch(e ->
                        e.getReason() == DistrictControlChangeEvent.ChangeReason.NEUTRAL_RESET),
                "a neutral reset must be announced as a control change");
    }

    @Test
    void captureThresholdSetsControllerAllianceAndFiresCaptureEvent() {
        playerInZone("AzurePlayer");
        tick(5); // 10 points required, 2 per tick

        var state = service.getControlState("contest_zone").get();
        assertEquals("azure_hearth", service.getControllerAlignment("contest_zone"));
        assertEquals(DistrictControlService.ContestState.COOLING_DOWN, state.state,
                "after capture the district cools down");
        assertTrue(events.stream().anyMatch(e ->
                e.getReason() == DistrictControlChangeEvent.ChangeReason.CAPTURE
                        && "azure_hearth".equals(e.getNewAlignmentId())
                        && "concordat_of_the_dawn".equals(e.getGrandAllianceId())));
        assertTrue(events.stream().anyMatch(e ->
                        e.getReason() == DistrictControlChangeEvent.ChangeReason.CAPTURE
                                && e.getDistrict() != null),
                "the event carries the district definition");
    }

    @Test
    void controlStatePersistsAcrossRepositoryReloads() {
        playerInZone("AzurePlayer");
        tick(5);
        repository.close();
        repository = null;
        // reopen a fresh repository over the same file
        var plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(dataDirectory.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("reload"));
        DistrictControlRepository reopened = new DistrictControlRepository(plugin);
        try {
            var state = reopened.getState("contest_zone");
            assertTrue(state.isPresent());
            assertEquals("azure_hearth", state.get().controllerAlignmentId());
        } finally {
            reopened.close();
            repository = new DistrictControlRepository(plugin);
            // restore the fixture repository to keep @AfterEach safe
        }
    }

    @Test
    void startupMergesPersistedControllerIntoDistrictOwnership() {
        repository.saveState(new ControlStateRecord(
                "contest_zone", "spirewrights", "ironclad_syndicate", "stable", null, 0, 1L));

        service.load();

        DistrictOwnership ownership = configManager.getDistrict("contest_zone").getOwnership();
        assertEquals("spirewrights", ownership.alignmentOwner(),
                "the persisted controller must be merged into the runtime district ownership");
        assertEquals("ironclad_syndicate", ownership.grandAllianceOwner());
        assertTrue(events.stream().anyMatch(e ->
                e.getReason() == DistrictControlChangeEvent.ChangeReason.OVERRIDE_MERGE));
    }

    @Test
    void adminSetControlChangesControllerRecordsHistoryAndFiresEvent() {
        service.adminSetControl("contest_zone", "spirewrights", "TesterAdmin");

        assertEquals("spirewrights", service.getControllerAlignment("contest_zone"));
        assertEquals("ironclad_syndicate", configManager.getDistrict("contest_zone")
                .getOwnership().grandAllianceOwner(),
                "set control derives the grand alliance from alignments.yml");
        assertTrue(events.stream().anyMatch(e ->
                e.getReason() == DistrictControlChangeEvent.ChangeReason.ADMIN_SET));

        List<ControlStateRecord> unused = List.of();
        var history = service.getRepository().getHistory("contest_zone", 10);
        assertTrue(history.stream().anyMatch(h -> "admin_set".equals(h.reason())));
    }

    @Test
    void adminClearAndLockControlBehaveAsConfigured() {
        service.adminSetControl("contest_zone", "azure_hearth", "TesterAdmin");

        service.adminLock("contest_zone", "TesterAdmin");
        assertEquals(DistrictControlService.ContestState.LOCKED,
                service.getControlState("contest_zone").get().state);

        service.adminUnlock("contest_zone", "TesterAdmin");
        assertEquals(DistrictControlService.ContestState.STABLE,
                service.getControlState("contest_zone").get().state);

        service.adminClearControl("contest_zone", "TesterAdmin");
        assertNull(service.getControllerAlignment("contest_zone"));
        assertTrue(events.stream().anyMatch(e ->
                e.getReason() == DistrictControlChangeEvent.ChangeReason.ADMIN_CLEAR));
    }
}
