package com.sunwayMinecraft.residency.admin;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class AdminSelectionManager {
    private static final String WAND_NAME = "§bResidency Unit Wand";
    private final Map<UUID, SelectionSession> sessions = new HashMap<>();

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

