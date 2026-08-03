package com.sunwayMinecraft.contracts.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ContractDomainTest {
    @Test
    void legacyDefinitionsUseSafeCategoryBasedObjectiveDefaults() {
        assertEquals(ContractObjectiveType.DELIVER_MATERIALS, ContractDefinition.defaultObjectiveType(ContractCategory.HAULING));
        assertEquals(ContractObjectiveType.DELIVER_MATERIALS, ContractDefinition.defaultObjectiveType(ContractCategory.RECOVERY));
        assertEquals(ContractObjectiveType.INTERACT_AT_DESTINATION, ContractDefinition.defaultObjectiveType(ContractCategory.MAINTENANCE));
        assertEquals(ContractObjectiveType.REACH_DESTINATION, ContractDefinition.defaultObjectiveType(ContractCategory.COURIER));
        assertEquals(ContractObjectiveType.REACH_DESTINATION, ContractDefinition.defaultObjectiveType(ContractCategory.SURVEY));
    }

    @Test
    void progressIsBoundedAndMarksCompletionAtOne() {
        ActiveContract contract = new ActiveContract(UUID.randomUUID(), "test", Instant.now(), Instant.now().plusSeconds(60));
        contract.setProgress(-0.25);
        assertEquals(0.0, contract.getProgress());
        contract.setProgress(2.0);
        assertEquals(1.0, contract.getProgress());
        assertTrue(contract.isObjectiveComplete());
    }
}
