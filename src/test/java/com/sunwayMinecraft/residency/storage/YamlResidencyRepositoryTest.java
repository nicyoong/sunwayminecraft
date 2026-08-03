package com.sunwayMinecraft.residency.storage;

import com.sunwayMinecraft.residency.domain.BillingPeriod;
import com.sunwayMinecraft.residency.domain.LeaseState;
import com.sunwayMinecraft.residency.domain.RentState;
import com.sunwayMinecraft.residency.domain.UnitTenancyRecord;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class YamlResidencyRepositoryTest {
    @TempDir
    Path dataDirectory;

    @Test
    void tenancyStateRoundTripsAcrossRepositoryRestart() {
        JavaPlugin plugin = plugin();
        YamlResidencyRepository writer = new YamlResidencyRepository(plugin);
        writer.load();
        UnitTenancyRecord record = writer.getTenancy("Unit-A");
        UUID tenant = UUID.randomUUID();
        Instant due = Instant.parse("2026-01-01T00:00:00Z");
        record.setTenantPlayerId(tenant);
        record.setLeaseState(LeaseState.ACTIVE);
        record.setRentState(RentState.CURRENT);
        record.setDepositAmount(50);
        record.setRentAmount(100);
        record.setBillingPeriod(BillingPeriod.MONTHLY);
        record.setNextDueAt(due);
        record.setApprovalRequired(true);
        writer.saveTenancy(record);

        YamlResidencyRepository reader = new YamlResidencyRepository(plugin);
        reader.load();
        UnitTenancyRecord restored = reader.getTenancy("unit-a");
        assertEquals(tenant, restored.getTenantPlayerId());
        assertEquals(LeaseState.ACTIVE, restored.getLeaseState());
        assertEquals(RentState.CURRENT, restored.getRentState());
        assertEquals(50, restored.getDepositAmount());
        assertEquals(100, restored.getRentAmount());
        assertEquals(BillingPeriod.MONTHLY, restored.getBillingPeriod());
        assertEquals(due, restored.getNextDueAt());
        assertTrue(restored.isApprovalRequired());
    }

    private JavaPlugin plugin() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(dataDirectory.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("YamlResidencyRepositoryTest"));
        return plugin;
    }
}
