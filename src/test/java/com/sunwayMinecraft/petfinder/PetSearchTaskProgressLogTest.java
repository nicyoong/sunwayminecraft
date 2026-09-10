package com.sunwayMinecraft.petfinder;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PetSearchTaskProgressLogTest {
    private final List<LogRecord> infoRecords = new ArrayList<>();

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void progressLogsOnlyWhenChunkCountAdvancesAndNeverAfterCompletion() {
        ServerMock server = MockBukkit.mock();
        WorldMock world = server.addSimpleWorld("world");
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getLogger()).thenReturn(capturingLogger());
        PetFinderManager manager = mock(PetFinderManager.class);
        CommandSender sender = mock(CommandSender.class);

        // 10 chunks of 50 entities, then an 11th chunk of 300: the tracker counts the
        // chunk of the next unprocessed entity, so the processed-chunk count reaches 10
        // on run 10 and then stalls while the remaining runs grind through chunk 11.
        List<Entity> entities = new ArrayList<>();
        for (int chunk = 0; chunk < 11; chunk++) {
            int entitiesInChunk = chunk < 10 ? 50 : 300;
            for (int i = 0; i < entitiesInChunk; i++) {
                Entity entity = mock(Entity.class);
                when(entity.getLocation()).thenReturn(new Location(world, (chunk + 1) * 100, 64, 0));
                entities.add(entity);
            }
        }

        PetSearchTask task = new PetSearchTask(plugin, sender, entities, null, null, manager, 800);
        task.runTaskTimer(plugin, 1L, 1L);
        server.getScheduler().performTicks(16);

        verify(manager).setSearchComplete();
        long progressLogs = infoRecords.stream()
                .filter(record -> record.getMessage().startsWith("Total chunks:"))
                .count();
        assertEquals(1, progressLogs,
                "progress should log once per 10-chunk advance, not every tick, and never after completion");
    }

    private Logger capturingLogger() {
        Logger logger = Logger.getLogger("PetSearchTaskProgressLogTest");
        logger.setLevel(Level.ALL);
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record.getLevel() == Level.INFO) {
                    infoRecords.add(record);
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
        return logger;
    }
}
