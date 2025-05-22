package com.sunwayMinecraft.switches;

import org.bukkit.Location;
import java.util.List;

public record ButtonSwitch(Location buttonLocation, List<Location> lightLocations) {}
