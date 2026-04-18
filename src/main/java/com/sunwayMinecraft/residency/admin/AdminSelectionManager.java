package com.sunwayMinecraft.residency.admin;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AdminSelectionManager {
    private static final String WAND_ID = "residency_unit_wand";

    private final JavaPlugin plugin;
    private final NamespacedKey wandKey;
    private final Map<UUID, SelectionSession> sessions = new HashMap<>();

    public AdminSelectionManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.wandKey = new NamespacedKey(plugin, WAND_ID);
    }

    public SelectionSession getSession(UUID uuid) {
        return sessions.computeIfAbsent(uuid, k -> new SelectionSession());
    }

    public void setPos1(UUID uuid, Location location) {
        getSession(uuid).setPos1(location);
    }

    public void setPos2(UUID uuid, Location location) {
        getSession(uuid).setPos2(location);
    }

    public void clear(UUID uuid) {
        sessions.remove(uuid);
    }

    public ItemStack createWandItem() {
        ItemStack item = new ItemStack(Material.IRON_SHOVEL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Residency Unit Wand"));
            meta.lore(List.of(
                    Component.text("Left-click block: set pos1"),
                    Component.text("Right-click block: set pos2"),
                    Component.text("Used for /resadmin createunit")
            ));
            meta.getPersistentDataContainer().set(wandKey, PersistentDataType.STRING, WAND_ID);
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isWand(ItemStack item) {
        if (item == null || item.getType() != Material.IRON_SHOVEL || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        String value = meta.getPersistentDataContainer().get(wandKey, PersistentDataType.STRING);
        return WAND_ID.equals(value);
    }
}
