package com.sunwayMinecraft.events.service;

import com.sunwayMinecraft.events.config.EventConfigManager;
import com.sunwayMinecraft.events.config.EventSettingsManager;
import com.sunwayMinecraft.events.domain.ActiveCityEvent;
import com.sunwayMinecraft.events.domain.CityEventDefinition;
import com.sunwayMinecraft.events.domain.CityEventType;
import com.sunwayMinecraft.events.domain.EventScope;
import com.sunwayMinecraft.events.persistence.EventPersistenceService;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CityEventsManagerTest {
    private ServerMock server;
    private JavaPlugin plugin;
    private EventConfigManager configManager;
    private EventSettingsManager settingsManager;
    private EventPersistenceService persistence;
    private CityEventsManager manager;

    @BeforeEach
    public void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();
        
        configManager = mock(EventConfigManager.class);
        settingsManager = mock(EventSettingsManager.class);
        persistence = mock(EventPersistenceService.class);
        
        when(persistence.load()).thenReturn(Collections.emptyList());
        when(settingsManager.getMaxActiveEvents()).thenReturn(1);
        when(settingsManager.isAutoAnnouncements()).thenReturn(false);

        manager = new CityEventsManager(plugin, configManager, settingsManager, persistence);
        manager.initialize();
    }

    @AfterEach
    public void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    public void testStartEvent() {
        String eventId = "test_event";
        CityEventDefinition def = new CityEventDefinition(
            eventId, CityEventType.SUPPLY_DRIVE, "Test Event", "Desc", EventScope.CITY, 1.5, new HashSet<>(), 60
        );
        when(configManager.getEvent(eventId)).thenReturn(def);

        boolean started = manager.startEvent(eventId, 60, ActiveCityEvent.TriggerMode.ADMIN);
        
        assertTrue(started);
        assertTrue(manager.isEventActive(eventId));
        assertEquals(1, manager.getActiveEvents().size());
        verify(persistence).save(anyList());
    }

    @Test
    public void testStartDuplicateEvent() {
        String eventId = "test_event";
        CityEventDefinition def = new CityEventDefinition(
            eventId, CityEventType.SUPPLY_DRIVE, "Test Event", "Desc", EventScope.CITY, 1.5, new HashSet<>(), 60
        );
        when(configManager.getEvent(eventId)).thenReturn(def);

        manager.startEvent(eventId, 60, ActiveCityEvent.TriggerMode.ADMIN);
        boolean secondStart = manager.startEvent(eventId, 60, ActiveCityEvent.TriggerMode.ADMIN);
        
        assertFalse(secondStart);
        assertEquals(1, manager.getActiveEvents().size());
    }

    @Test
    public void testMaxEventsLimit() {
        when(settingsManager.getMaxActiveEvents()).thenReturn(1);
        
        String e1 = "event1";
        String e2 = "event2";
        
        when(configManager.getEvent(e1)).thenReturn(mock(CityEventDefinition.class));
        when(configManager.getEvent(e2)).thenReturn(mock(CityEventDefinition.class));

        manager.startEvent(e1, 60, ActiveCityEvent.TriggerMode.ADMIN);
        boolean startedSecond = manager.startEvent(e2, 60, ActiveCityEvent.TriggerMode.ADMIN);
        
        assertFalse(startedSecond);
        assertEquals(1, manager.getActiveEvents().size());
    }

    @Test
    public void testStopEvent() {
        String eventId = "test_event";
        when(configManager.getEvent(eventId)).thenReturn(mock(CityEventDefinition.class));
        
        manager.startEvent(eventId, 60, ActiveCityEvent.TriggerMode.ADMIN);
        boolean stopped = manager.stopEvent(eventId);
        
        assertTrue(stopped);
        assertFalse(manager.isEventActive(eventId));
        verify(persistence, times(2)).save(anyList()); // once on start, once on stop
    }
}
