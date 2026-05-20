package com.sunwayMinecraft.beacon;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.plugin.PluginMock;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class ColorTransitionManagerTest {
    private ServerMock server;
    private PluginMock plugin;
    private ColorTransitionManager manager;
    private Location beaconLocation;
    private List<Material> colorCycle;
    private Map<Location, Integer> beaconColors;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();
        
        World world = server.addSimpleWorld("test_world");
        beaconLocation = new Location(world, 0, 64, 0);
        beaconLocation.getBlock().setType(Material.BEACON);
        
        // Prepare glass blocks above beacon
        for (int i = 1; i <= 5; i++) {
            beaconLocation.clone().add(0, i, 0).getBlock().setType(Material.GLASS);
        }

        colorCycle = Arrays.asList(Material.RED_STAINED_GLASS, Material.BLUE_STAINED_GLASS);
        beaconColors = new HashMap<>();
        beaconColors.put(beaconLocation, 0);

        manager = new ColorTransitionManager(plugin, beaconColors, colorCycle);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void testStartTransitionSchedulesTask() {
        manager.startTransition(plugin, 10, beaconColors);
        server.getScheduler().performOneTick();
        
        // Initially, some blocks might change immediately because task runs at 0 delay
        // currentBinaryCycle starts at 0.
        // binarySequence[0] = 0.
        // for i=0 to 4: bitState = (0 & (1<<i)) != 0 ? 1 : 0 -> 0.
        // so all 5 blocks set to oldColor. 
        // oldColor = colorCycle.get((0 + 2 - 1) % 2) = colorCycle.get(1) = BLUE_STAINED_GLASS.
        
        assertEquals(Material.BLUE_STAINED_GLASS, beaconLocation.clone().add(0, 5, 0).getBlock().getType());
        assertEquals(Material.BLUE_STAINED_GLASS, beaconLocation.clone().add(0, 4, 0).getBlock().getType());
        assertEquals(Material.BLUE_STAINED_GLASS, beaconLocation.clone().add(0, 3, 0).getBlock().getType());
        assertEquals(Material.BLUE_STAINED_GLASS, beaconLocation.clone().add(0, 2, 0).getBlock().getType());
        assertEquals(Material.BLUE_STAINED_GLASS, beaconLocation.clone().add(0, 1, 0).getBlock().getType());
    }

    @Test
    void testMaterialChangesAsTransitionProgresses() {
        manager.startTransition(plugin, 10, beaconColors);
        server.getScheduler().performOneTick(); // Run tick 0 (cycle 0)
        
        server.getScheduler().performTicks(10L); // Run tick 10 (cycle 1)
        
        // binarySequence[1] = 16 = 00010000 (binary). Bit 4 is set.
        // i=4 corresponds to location + (5-4) = location + 1.
        assertEquals(Material.RED_STAINED_GLASS, beaconLocation.clone().add(0, 1, 0).getBlock().getType(), "Bottom glass should be RED");
        assertEquals(Material.BLUE_STAINED_GLASS, beaconLocation.clone().add(0, 5, 0).getBlock().getType(), "Top glass should be BLUE");
    }

    @Test
    void testPauseStopsTransition() {
        manager.startTransition(plugin, 10, beaconColors);
        server.getScheduler().performOneTick(); // Run tick 0
        
        manager.pause();
        
        server.getScheduler().performTicks(10L);
        
        // Material should NOT have changed to the next cycle state if it was paused
        // It should stay at binaryCycle 0 state (all BLUE).
        assertEquals(Material.BLUE_STAINED_GLASS, beaconLocation.clone().add(0, 1, 0).getBlock().getType());
    }

    @Test
    void testResumeRestartsTransition() {
        manager.startTransition(plugin, 10, beaconColors);
        server.getScheduler().performOneTick(); // Run tick 0
        
        manager.pause();
        manager.resume(plugin);
        
        server.getScheduler().performTicks(10L); // Run tick 10
        
        // It should have progressed to cycle 1
        assertEquals(Material.RED_STAINED_GLASS, beaconLocation.clone().add(0, 1, 0).getBlock().getType());
    }

    @Test
    void testFullCycleMaterialChanges() {
        manager.startTransition(plugin, 1, beaconColors);
        server.getScheduler().performOneTick(); // Run tick 0
        
        // After 16 more ticks, currentColorIndex should increment.
        // Tick 16: task runs for the 17th time (index 16).
        // Wait, currentBinaryCycle goes 0 to 15.
        // Run 1: index 0, cycle becomes 1.
        // Run 16: index 15, cycle becomes 16 -> 0, currentColorIndex becomes 1.
        // Run 17: index 0 (with currentColorIndex 1).
        
        server.getScheduler().performTicks(16L);
        
        // At this point, it has run 17 times.
        // Run 17 is at currentBinaryCycle 0, currentColorIndex 1.
        // newColor = colorCycle[1] = BLUE.
        // oldColor = colorCycle[0] = RED.
        // binarySequence[0] = 0. All blocks set to oldColor (RED).
        
        assertEquals(Material.RED_STAINED_GLASS, beaconLocation.clone().add(0, 1, 0).getBlock().getType());
        assertEquals(Material.RED_STAINED_GLASS, beaconLocation.clone().add(0, 5, 0).getBlock().getType());
    }
}
