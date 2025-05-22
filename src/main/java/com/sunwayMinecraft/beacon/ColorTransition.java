package com.sunwayMinecraft.beacon;

import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

interface ColorTransition {
  void startTransition(
      JavaPlugin plugin, int ticksPerTransition, Map<Location, Integer> beaconColors);

  void pause();

  void resume(JavaPlugin plugin);

  long getTicksPerTransition();
}
