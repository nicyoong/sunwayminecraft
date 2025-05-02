package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.switches.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class SwitchesCommands {
    private final JavaPlugin plugin;
    private final LightConfigManager lightConfig;
    private final SwitchConfigManager switchConfig;
    private final LightManager lightManager;
    private SwitchManager switchManager;

    // Changed to match benches pattern
    public SwitchesCommands(JavaPlugin plugin,
                            LightConfigManager lightConfig,
                            SwitchConfigManager switchConfig) {
        this.plugin = plugin;
        this.lightConfig = lightConfig;
        this.switchConfig = switchConfig;
        this.lightManager = new LightManager(lightConfig);
        this.switchManager = new SwitchManager(switchConfig, lightConfig);
    }
}
