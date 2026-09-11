package com.sunwayMinecraft.alignments.listener;

import com.sunwayMinecraft.alignments.service.AlignmentMembershipCache;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Keeps the alignment membership cache in sync with online players:
 * loaded on join, dropped on quit.
 */
public class AlignmentPlayerListener implements Listener {
  private final AlignmentMembershipCache cache;

  public AlignmentPlayerListener(AlignmentMembershipCache cache) {
    this.cache = cache;
  }

  public void register(JavaPlugin plugin) {
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    cache.load(event.getPlayer().getUniqueId());
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    cache.invalidate(event.getPlayer().getUniqueId());
  }
}
