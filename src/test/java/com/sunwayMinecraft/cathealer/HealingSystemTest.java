package com.sunwayMinecraft.cathealer;

import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.CatMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.plugin.PluginMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HealingSystemTest {
    private ServerMock server;
    private PluginMock plugin;
    private HealingSystem healingSystem;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();
        healingSystem = new HealingSystem(plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void testHealingWhenCatNearby() {
        PlayerMock player = server.addPlayer();
        player.setHealth(10.0);
        
        Cat cat = player.getWorld().spawn(player.getLocation(), Cat.class);
        cat.setTamed(true);
        cat.setOwner(player);
        cat.setSitting(false);

        healingSystem.start();
        server.getScheduler().performTicks(50L);

        assertEquals(11.0, player.getHealth(), "Player should be healed by 1 point");
    }

    @Test
    void testNoHealingWhenCatNotTamed() {
        PlayerMock player = server.addPlayer();
        player.setHealth(10.0);
        
        Cat cat = player.getWorld().spawn(player.getLocation(), Cat.class);
        cat.setTamed(false);
        // Do not set owner for untamed cat
        cat.setSitting(false);

        healingSystem.start();
        server.getScheduler().performTicks(50L);

        assertEquals(10.0, player.getHealth(), "Player should not be healed if cat is not tamed");
    }

    @Test
    void testNoHealingWhenCatNotOwned() {
        PlayerMock player = server.addPlayer();
        PlayerMock otherPlayer = server.addPlayer();
        player.setHealth(10.0);
        
        Cat cat = player.getWorld().spawn(player.getLocation(), Cat.class);
        cat.setTamed(true);
        cat.setOwner(otherPlayer);
        cat.setSitting(false);

        healingSystem.start();
        server.getScheduler().performTicks(50L);

        assertEquals(10.0, player.getHealth(), "Player should not be healed if cat is owned by someone else");
    }

    @Test
    void testNoHealingWhenCatSitting() {
        PlayerMock player = server.addPlayer();
        player.setHealth(10.0);
        
        Cat cat = player.getWorld().spawn(player.getLocation(), Cat.class);
        cat.setTamed(true);
        cat.setOwner(player);
        cat.setSitting(true);

        healingSystem.start();
        server.getScheduler().performTicks(50L);

        assertEquals(10.0, player.getHealth(), "Player should not be healed if cat is sitting");
    }

    @Test
    void testNoHealingWhenCatTooFar() {
        PlayerMock player = server.addPlayer();
        player.setHealth(10.0);
        
        Location farLocation = player.getLocation().clone().add(15, 0, 0);
        Cat cat = player.getWorld().spawn(farLocation, Cat.class);
        cat.setTamed(true);
        cat.setOwner(player);
        cat.setSitting(false);

        healingSystem.start();
        server.getScheduler().performTicks(50L);

        assertEquals(10.0, player.getHealth(), "Player should not be healed if cat is too far");
    }

    @Test
    void testHealingDoesNotExceedMaxHealth() {
        PlayerMock player = server.addPlayer();
        double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
        player.setHealth(maxHealth - 0.5);
        
        Cat cat = player.getWorld().spawn(player.getLocation(), Cat.class);
        cat.setTamed(true);
        cat.setOwner(player);
        cat.setSitting(false);

        healingSystem.start();
        server.getScheduler().performTicks(50L);

        assertEquals(maxHealth, player.getHealth(), "Player health should not exceed max health");
    }

    @Test
    void testMultipleCatsProvideMoreHealing() {
        PlayerMock player = server.addPlayer();
        player.setHealth(10.0);
        
        for (int i = 0; i < 3; i++) {
            Cat cat = player.getWorld().spawn(player.getLocation(), Cat.class);
            cat.setTamed(true);
            cat.setOwner(player);
            cat.setSitting(false);
        }

        healingSystem.start();
        server.getScheduler().performTicks(50L);

        assertEquals(13.0, player.getHealth(), "Player should be healed by 3 points from 3 cats");
    }
}
