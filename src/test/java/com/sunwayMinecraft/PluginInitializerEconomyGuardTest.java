package com.sunwayMinecraft;

import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicesManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PluginInitializerEconomyGuardTest {
    private final List<PluginInitializer> createdInitializers = new ArrayList<>();
    // The initializer registers a PlayerJoinEvent listener with the mocked
    // plugin; MockBukkit's unmock does not clear static handler lists, so the
    // registration must be removed manually or later addPlayer calls NPE.
    private SunwayMinecraft registeredPlugin;
    @TempDir
    Path dataDirectory;

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        server.addSimpleWorld("world");
        server.addSimpleWorld("world_nether");
    }

    @AfterEach
    void tearDown() {
        for (PluginInitializer initializer : createdInitializers) {
            // close SQLite connections so @TempDir cleanup works on Windows
            if (initializer.getCoinFlipDatabase() != null) {
                initializer.getCoinFlipDatabase().close();
            }
            if (initializer.getDistrictControlRepository() != null) {
                initializer.getDistrictControlRepository().close();
            }
        }

        org.bukkit.event.HandlerList.unregisterAll();
        MockBukkit.unmock();
    }

    @Test
    void startupSurvivesVaultPresentWithoutEconomyRegistration() throws Exception {
        SunwayMinecraft plugin = pluginMock();
        registeredPlugin = plugin;

        PluginInitializer initializer = new PluginInitializer(plugin);
        createdInitializers.add(initializer);

        assertNull(initializer.getCoinFlipSystem(),
                "coinflip must disable itself when no economy provider is registered");
        assertNotNull(initializer.getResidencyManager(),
                "residency must initialize without an economy provider");
        assertNotNull(initializer.getContractsManager(),
                "contracts must initialize without an economy provider");
        assertTrue(severeRecords.stream().anyMatch(record ->
                        record.getMessage().contains("no economy provider is registered")),
                "expected a severe log explaining the missing economy registration");
    }

    private final List<LogRecord> severeRecords = new ArrayList<>();

    private SunwayMinecraft pluginMock() {
        Server serverMock = mock(Server.class);
        PluginManager pluginManager = mock(PluginManager.class);
        when(pluginManager.getPlugin("Vault")).thenReturn(mock(org.bukkit.plugin.Plugin.class));
        ServicesManager servicesManager = mock(ServicesManager.class);
        when(serverMock.getPluginManager()).thenReturn(pluginManager);
        when(serverMock.getScheduler()).thenReturn(server.getScheduler());
        when(serverMock.getServicesManager()).thenReturn(servicesManager);

        SunwayMinecraft plugin = mock(SunwayMinecraft.class);
        when(plugin.getServer()).thenReturn(serverMock);
        when(plugin.getName()).thenReturn("SunwayMinecraft");
        when(plugin.namespace()).thenReturn("sunwayminecraft");
        when(plugin.isEnabled()).thenReturn(true);
        Logger logger = Logger.getLogger("PluginInitializerEconomyGuardTest");
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record.getLevel() == Level.SEVERE) {
                    severeRecords.add(record);
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
        when(plugin.getLogger()).thenReturn(logger);
        when(plugin.getDataFolder()).thenReturn(dataDirectory.toFile());
        when(plugin.getConfig()).thenAnswer(invocation -> new YamlConfiguration());
        doAnswer(invocation -> {
            String name = invocation.getArgument(0);
            File out = new File(dataDirectory.toFile(), name);
            if (!out.exists()) {
                try (InputStream in = getClass().getClassLoader().getResourceAsStream(name)) {
                    if (in != null) {
                        Files.copy(in, out.toPath());
                    }
                }
            }
            return null;
        }).when(plugin).saveResource(anyString(), anyBoolean());
        return plugin;
    }
}
