package com.sunwayMinecraft.events.service;

import com.sunwayMinecraft.contracts.domain.ContractCategory;
import com.sunwayMinecraft.events.config.EventConfigManager;
import com.sunwayMinecraft.events.domain.ActiveCityEvent;
import com.sunwayMinecraft.events.domain.CityEventDefinition;
import com.sunwayMinecraft.events.domain.CityEventType;
import com.sunwayMinecraft.events.domain.EventScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class EventModifierServiceTest {
    private CityEventsManager eventsManager;
    private EventConfigManager configManager;
    private EventModifierService modifierService;

    @BeforeEach
    public void setUp() {
        eventsManager = mock(CityEventsManager.class);
        configManager = mock(EventConfigManager.class);
        when(eventsManager.getConfigManager()).thenReturn(configManager);
        modifierService = new EventModifierService(eventsManager);
    }

    @Test
    public void testGetRewardMultiplierNoEvents() {
        when(eventsManager.getActiveEvents()).thenReturn(List.of());
        assertEquals(1.0, modifierService.getRewardMultiplier(ContractCategory.HAULING));
    }

    @Test
    public void testGetRewardMultiplierWithEvent() {
        String eventId = "supply_drive";
        CityEventDefinition def = new CityEventDefinition(
            eventId, CityEventType.SUPPLY_DRIVE, "Supply Drive", "Desc", 
            EventScope.CITY, 1.5, Set.of(ContractCategory.HAULING), 60
        );
        
        ActiveCityEvent active = new ActiveCityEvent(eventId, Instant.now(), Instant.now().plusSeconds(3600), ActiveCityEvent.TriggerMode.ADMIN);
        
        when(eventsManager.getActiveEvents()).thenReturn(List.of(active));
        when(configManager.getEvent(eventId)).thenReturn(def);

        assertEquals(1.5, modifierService.getRewardMultiplier(ContractCategory.HAULING));
        assertEquals(1.0, modifierService.getRewardMultiplier(ContractCategory.MAINTENANCE));
    }

    @Test
    public void testGetHighestMultiplier() {
        String e1 = "e1";
        String e2 = "e2";
        
        CityEventDefinition d1 = new CityEventDefinition(e1, CityEventType.SUPPLY_DRIVE, "N1", "D", EventScope.CITY, 1.2, Set.of(ContractCategory.HAULING), 60);
        CityEventDefinition d2 = new CityEventDefinition(e2, CityEventType.MARKET_WEEK, "N2", "D", EventScope.CITY, 1.5, Set.of(ContractCategory.HAULING), 60);
        
        ActiveCityEvent a1 = new ActiveCityEvent(e1, Instant.now(), Instant.now().plusSeconds(3600), ActiveCityEvent.TriggerMode.ADMIN);
        ActiveCityEvent a2 = new ActiveCityEvent(e2, Instant.now(), Instant.now().plusSeconds(3600), ActiveCityEvent.TriggerMode.ADMIN);
        
        when(eventsManager.getActiveEvents()).thenReturn(List.of(a1, a2));
        when(configManager.getEvent(e1)).thenReturn(d1);
        when(configManager.getEvent(e2)).thenReturn(d2);

        assertEquals(1.5, modifierService.getRewardMultiplier(ContractCategory.HAULING));
    }

    @Test
    public void testGetPrimaryEvent() {
        String eventId = "supply_drive";
        CityEventDefinition def = new CityEventDefinition(
            eventId, CityEventType.SUPPLY_DRIVE, "Supply Drive", "Desc", 
            EventScope.CITY, 1.5, Set.of(ContractCategory.HAULING), 60
        );
        
        ActiveCityEvent active = new ActiveCityEvent(eventId, Instant.now(), Instant.now().plusSeconds(3600), ActiveCityEvent.TriggerMode.ADMIN);
        
        when(eventsManager.getActiveEvents()).thenReturn(List.of(active));
        when(configManager.getEvent(eventId)).thenReturn(def);

        Optional<CityEventDefinition> result = modifierService.getPrimaryEventForCategory(ContractCategory.HAULING);
        assertTrue(result.isPresent());
        assertEquals(eventId, result.get().id());
    }
}
