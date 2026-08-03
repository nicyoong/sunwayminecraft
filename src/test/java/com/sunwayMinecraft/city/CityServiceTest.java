package com.sunwayMinecraft.city;

import com.sunwayMinecraft.PluginInitializer;
import com.sunwayMinecraft.contracts.config.ContractConfigManager;
import com.sunwayMinecraft.contracts.domain.ContractCategory;
import com.sunwayMinecraft.contracts.domain.ContractDefinition;
import com.sunwayMinecraft.contracts.service.ContractsManager;
import com.sunwayMinecraft.districts.DistrictManager;
import com.sunwayMinecraft.districts.domain.ApprovalBias;
import com.sunwayMinecraft.districts.domain.DistrictDefinition;
import com.sunwayMinecraft.districts.domain.DistrictType;
import com.sunwayMinecraft.districts.region.Region3i;
import com.sunwayMinecraft.events.domain.ActiveCityEvent;
import com.sunwayMinecraft.events.domain.CityEventDefinition;
import com.sunwayMinecraft.events.domain.CityEventType;
import com.sunwayMinecraft.events.domain.EventScope;
import com.sunwayMinecraft.events.service.CityEventsManager;
import com.sunwayMinecraft.events.service.EventModifierService;
import com.sunwayMinecraft.residency.ResidencyManager;
import com.sunwayMinecraft.residency.domain.UnitTenancyRecord;
import com.sunwayMinecraft.residency.storage.ResidencyRepository;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CityServiceTest {
    @Test
    void overviewCombinesCurrentCityStateAndUsesSafeUnavailableFallbacks() {
        ServerMock server = MockBukkit.mock();
        try {
            PlayerMock player = server.addPlayer();
            PluginInitializer init = mock(PluginInitializer.class);
            CityEventsManager events = mock(CityEventsManager.class);
            com.sunwayMinecraft.events.config.EventConfigManager eventConfig = mock(com.sunwayMinecraft.events.config.EventConfigManager.class);
            CityEventDefinition event = new CityEventDefinition("supply", CityEventType.SUPPLY_DRIVE, "Supply drive", "",
                    EventScope.CITY, 1.5, Set.of(ContractCategory.HAULING), 60);
            when(events.getActiveEvents()).thenReturn(List.of(new ActiveCityEvent("supply", Instant.now(), Instant.now().plusSeconds(60), ActiveCityEvent.TriggerMode.ADMIN)));
            when(events.getConfigManager()).thenReturn(eventConfig);
            when(eventConfig.getEvent("supply")).thenReturn(event);
            when(init.getCityEventsManager()).thenReturn(events);

            ContractsManager contracts = mock(ContractsManager.class);
            ContractConfigManager contractsConfig = mock(ContractConfigManager.class);
            ContractDefinition haul = new ContractDefinition("haul", ContractCategory.HAULING, "Haul", "", 1, 1, 1, "a", "b", Map.of(), "");
            ContractDefinition courier = new ContractDefinition("courier", ContractCategory.COURIER, "Courier", "", 1, 1, 1, "a", "b", Map.of(), "");
            when(contracts.getContractConfig()).thenReturn(contractsConfig);
            when(contractsConfig.getContracts()).thenReturn(Map.of("haul", haul, "courier", courier));
            when(init.getContractsManager()).thenReturn(contracts);
            EventModifierService modifiers = mock(EventModifierService.class);
            when(modifiers.getPrimaryEventForCategory(ContractCategory.HAULING)).thenReturn(java.util.Optional.of(event));
            when(modifiers.getPrimaryEventForCategory(ContractCategory.COURIER)).thenReturn(java.util.Optional.empty());
            when(init.getEventModifierService()).thenReturn(modifiers);

            DistrictManager districts = mock(DistrictManager.class);
            DistrictDefinition district = new DistrictDefinition("central", "Central", null, "world", new Region3i("world", 0, 0, 0, 10, 10, 10), true,
                    DistrictType.RESIDENTIAL, 1, "summary", List.of(), true, 0, false, false, ApprovalBias.STANDARD, false, false);
            when(districts.getDistrictAt(player.getLocation())).thenReturn(district);
            when(init.getDistrictManager()).thenReturn(districts);

            ResidencyManager residency = mock(ResidencyManager.class);
            ResidencyRepository repository = mock(ResidencyRepository.class);
            UnitTenancyRecord tenancy = new UnitTenancyRecord("unit");
            tenancy.setTenantPlayerId(player.getUniqueId());
            when(residency.getRepository()).thenReturn(repository);
            when(repository.getAllTenancies()).thenReturn(List.of(tenancy));
            when(init.getResidencyManager()).thenReturn(residency);

            CityOverviewService overview = new CityOverviewService(init);
            assertEquals("Supply drive", overview.getCurrentEventName());
            assertEquals(2, overview.getAvailableContractsCount());
            assertEquals(1, overview.getBoostedContractsCount());
            assertEquals("Central", overview.getDistrictName(player));
            assertEquals("1 active rental(s)", overview.getResidencySummary(player));
        } finally {
            MockBukkit.unmock();
        }
    }

    @Test
    void validationReportsUnavailableSubsystemsAndPassesThroughAvailableIssues() {
        PluginInitializer init = mock(PluginInitializer.class);
        DistrictManager districts = mock(DistrictManager.class);
        ResidencyManager residency = mock(ResidencyManager.class);
        when(init.getDistrictManager()).thenReturn(districts);
        when(init.getResidencyManager()).thenReturn(residency);
        when(districts.validate()).thenReturn(List.of("overlap"));
        when(residency.validate()).thenReturn(List.of());

        Map<String, List<String>> results = new CityValidationService(init).runAll();

        assertIterableEquals(List.of("overlap"), results.get("Districts"));
        assertIterableEquals(List.of(), results.get("Residency"));
        assertIterableEquals(List.of("Subsystem unavailable"), results.get("Contracts"));
        assertIterableEquals(List.of("Subsystem unavailable"), results.get("Events"));
    }
}
