package com.sunwayMinecraft.beacon;

import org.bukkit.Location;
import org.bukkit.Material;

import java.util.List;
import java.util.Map;

interface ConfigurationLoader {
    int getTicksPerTransition();
    List<Material> getColorCycle();
    Map<Location, Integer> loadBeacons();
}