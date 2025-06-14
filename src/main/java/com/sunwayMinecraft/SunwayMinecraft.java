package com.sunwayMinecraft;

import com.sunwayMinecraft.utils.ConfigLoader;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Level;

public final class SunwayMinecraft extends JavaPlugin {

  private PluginInitializer initializer;
  private CommandRegistrar commandRegistrar;

  @Override
  public void onEnable() {
    getLogger().log(Level.INFO, "Enabling SunwayMinecraft plugin...");

    // Load main config & initialize all systems
    initializer = new PluginInitializer(this);

    // Register all commands in one place
    commandRegistrar = new CommandRegistrar(this);
    commandRegistrar.registerAll(initializer);

    getLogger().log(Level.INFO, "SunwayMinecraft plugin has been enabled.");
  }

  @Override
  public void onDisable() {
    if (initializer.getCoinFlipDatabase() != null) {
      initializer.getCoinFlipDatabase().close();
    }
    getLogger().log(Level.INFO, "Disabling SunwayMinecraft plugin...");
  }
}
