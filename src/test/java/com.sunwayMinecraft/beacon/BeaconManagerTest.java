package com.sunwayMinecraft.beacon;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.WorldMock;
import be.seeseemelk.mockbukkit.block.BlockMock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.junit.jupiter.api.*;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class BeaconManagerTest {

    private static ServerMock server;
    private static JavaPlugin plugin;
    private static WorldMock world;
    private BeaconManager beaconManager;

    @BeforeAll
    public static void setUpServer() {
        // Initialize the MockBukkit environment
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();
        world = new WorldMock();
    }

    @AfterAll
    public static void tearDownServer() {
        // Unload the MockBukkit environment
        MockBukkit.unmock();
    }

    @BeforeEach
    public void setUp() {
        // Set up the configuration for testing
        FileConfiguration config = plugin.getConfig();
        config.set("ticksPerTransition", 20);
        config.set("beaconColors", Arrays.asList("RED_STAINED_GLASS", "BLUE_STAINED_GLASS", "YELLOW_STAINED_GLASS"));

        // Initialize the BeaconManager with the mocked plugin
        beaconManager = new BeaconManager(plugin);
        beaconManager.initialize();
    }

    @Test
    @DisplayName("Verify that the color transition task starts correctly")
    void testColorTransitionTaskInitialization() {
        BukkitRunnable task = beaconManager.getColorTransitionTask();
        assertNotNull(task, "The color transition task should be initialized.");
    }

        @Test
    @DisplayName("Verify that the beacon color changes correctly")
    void testBeaconColorTransition() {
        // Create a location in the mocked world and place a beacon
        Location beaconLocation = new Location(world, 0, 64, 0);
        world.getBlockAt(beaconLocation).setType(Material.BEACON);

        // Place the initial old block above the beacon (starting with MAGENTA_STAINED_GLASS)
        for (int i = 1; i <= 4; i++) {
            Location aboveBeaconLocation = beaconLocation.clone().add(0, i, 0);
            world.getBlockAt(aboveBeaconLocation).setType(Material.MAGENTA_STAINED_GLASS); // Initial setup

            // Assert that the block is set to MAGENTA_STAINED_GLASS with an error message
            assertEquals(
                    Material.MAGENTA_STAINED_GLASS,
                    world.getBlockAt(aboveBeaconLocation).getType(),
                    "Expected block at " + aboveBeaconLocation + " to be MAGENTA_STAINED_GLASS, but it was "
                            + world.getBlockAt(aboveBeaconLocation).getType()
            );
        }

        // Register the beacon using a new method in BeaconManager
        beaconManager.addBeacon(beaconLocation);

        // Cycle Step 1: Transition from old block (MAGENTA) to new block (RED) on the topmost position
        int cycleStep1 = 1; // Arbitrary step to signify first transition
        beaconManager.testUpdateBeaconColor(beaconLocation, cycleStep1);
        assertEquals(Material.RED_STAINED_GLASS, world.getBlockAt(beaconLocation.clone().add(0, 4, 0)).getType(), "Block (0, 4, 0) should transition to RED_STAINED_GLASS.");
        assertEquals(Material.MAGENTA_STAINED_GLASS, world.getBlockAt(beaconLocation.clone().add(0, 3, 0)).getType(), "Block (0, 3, 0) should remain MAGENTA_STAINED_GLASS.");
        assertEquals(Material.MAGENTA_STAINED_GLASS, world.getBlockAt(beaconLocation.clone().add(0, 2, 0)).getType(), "Block (0, 2, 0) should remain MAGENTA_STAINED_GLASS.");
        assertEquals(Material.MAGENTA_STAINED_GLASS, world.getBlockAt(beaconLocation.clone().add(0, 1, 0)).getType(), "Block (0, 1, 0) should remain MAGENTA_STAINED_GLASS.");

        // Cycle Step 2: Transition from MAGENTA to RED on the next position
        int cycleStep2 = 2;
        beaconManager.testUpdateBeaconColor(beaconLocation, cycleStep2);
        assertEquals(Material.MAGENTA_STAINED_GLASS, world.getBlockAt(beaconLocation.clone().add(0, 4, 0)).getType(), "Block (0, 4, 0) should revert to MAGENTA_STAINED_GLASS.");
        assertEquals(Material.RED_STAINED_GLASS, world.getBlockAt(beaconLocation.clone().add(0, 3, 0)).getType(), "Block (0, 3, 0) should transition to RED_STAINED_GLASS.");
        assertEquals(Material.MAGENTA_STAINED_GLASS, world.getBlockAt(beaconLocation.clone().add(0, 2, 0)).getType(), "Block (0, 2, 0) should remain MAGENTA_STAINED_GLASS.");
        assertEquals(Material.MAGENTA_STAINED_GLASS, world.getBlockAt(beaconLocation.clone().add(0, 1, 0)).getType(), "Block (0, 1, 0) should remain MAGENTA_STAINED_GLASS.");

        // Cycle Step 3: Transition from MAGENTA to RED on the third position
        int cycleStep3 = 3;
        beaconManager.testUpdateBeaconColor(beaconLocation, cycleStep3);
        assertEquals(Material.RED_STAINED_GLASS, world.getBlockAt(beaconLocation.clone().add(0, 4, 0)).getType(), "Block (0, 4, 0) should transition to RED_STAINED_GLASS.");
        assertEquals(Material.RED_STAINED_GLASS, world.getBlockAt(beaconLocation.clone().add(0, 3, 0)).getType(), "Block (0, 3, 0) should transition to RED_STAINED_GLASS.");
        assertEquals(Material.MAGENTA_STAINED_GLASS, world.getBlockAt(beaconLocation.clone().add(0, 2, 0)).getType(), "Block (0, 2, 0) should remain MAGENTA_STAINED_GLASS.");
        assertEquals(Material.MAGENTA_STAINED_GLASS, world.getBlockAt(beaconLocation.clone().add(0, 1, 0)).getType(), "Block (0, 1, 0) should remain MAGENTA_STAINED_GLASS.");
    }

}
