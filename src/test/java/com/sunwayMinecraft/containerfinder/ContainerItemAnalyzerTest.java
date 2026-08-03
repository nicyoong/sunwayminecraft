package com.sunwayMinecraft.containerfinder;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContainerItemAnalyzerTest {
    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void groupsPlainItemsByMaterialAndMergesTheirCounts() {
        ContainerItemAnalyzer analyzer = new ContainerItemAnalyzer();
        ContainerItemAnalyzer.ItemGroup group = analyzer.toItemGroup(new ItemStack(Material.DIAMOND, 3));
        Map<String, Long> counts = new HashMap<>();
        Map<String, String> labels = new HashMap<>();

        analyzer.mergeCount(counts, labels, group);
        analyzer.mergeCount(counts, labels, analyzer.toItemGroup(new ItemStack(Material.DIAMOND, 2)));

        assertEquals("DIAMOND", group.key());
        assertEquals("Diamond", group.label());
        assertEquals(5L, counts.get("DIAMOND"));
        assertEquals("Diamond", labels.get("DIAMOND"));
    }

    @Test
    void customNameCreatesADistinctStableItemGroup() {
        ItemStack item = new ItemStack(Material.PAPER, 1);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Permit"));
        item.setItemMeta(meta);

        ContainerItemAnalyzer.ItemGroup group = new ContainerItemAnalyzer().toItemGroup(item);

        assertTrue(group.key().contains("PAPER|Name=Permit"));
        assertTrue(group.label().contains("Paper {Name=Permit}"));
    }
}
