package com.sunwayMinecraft.alignments.config;

import com.sunwayMinecraft.alignments.domain.AlignmentDefinition;
import com.sunwayMinecraft.alignments.domain.Campus;
import com.sunwayMinecraft.alignments.domain.GrandAlliance;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AlignmentConfigManagerTest {
    @TempDir
    Path dataDirectory;

    @Test
    void missingConfigFileIsGeneratedFromDefaultResource() {
        JavaPlugin plugin = pluginFor(dataDirectory);
        AlignmentConfigManager manager = new AlignmentConfigManager(plugin);
        manager.load();

        assertTrue(Files.exists(dataDirectory.resolve("alignments.yml")),
                "missing alignments.yml must be regenerated from the default resource");
        assertEquals(6, manager.getAllAlignments().size());
    }

    @Test
    void defaultConfigLoadsAllAlliancesAndCampusGroupings() {
        JavaPlugin plugin = pluginFor(dataDirectory);
        AlignmentConfigManager manager = new AlignmentConfigManager(plugin);
        manager.load();

        Optional<AlignmentDefinition> azureHearth = manager.getAlignment("azure_hearth");
        assertTrue(azureHearth.isPresent());
        assertEquals("Azure Hearth", azureHearth.get().displayName());
        assertEquals(GrandAlliance.CONCORDAT_OF_THE_DAWN, azureHearth.get().grandAlliance());
        assertEquals(Campus.TAYLORS, azureHearth.get().homeCampus());
        assertTrue(azureHearth.get().enabled());

        Optional<AlignmentDefinition> unknown = manager.getAlignment("does_not_exist");
        assertFalse(unknown.isPresent());

        assertEquals(6, manager.getEnabledAlignments().size());
        assertEquals(3, manager.getAlignmentsByGrandAlliance(GrandAlliance.CONCORDAT_OF_THE_DAWN).size());
        assertEquals(3, manager.getAlignmentsByGrandAlliance(GrandAlliance.IRONCLAD_SYNDICATE).size());
        assertEquals(2, manager.getAlignmentsByCampus(Campus.TAYLORS).size());
        assertEquals(2, manager.getAlignmentsByCampus(Campus.SUNWAY).size());
        assertEquals(2, manager.getAlignmentsByCampus(Campus.MONASH).size());
    }

    @Test
    void alignmentsReferencingUnknownAllianceOrCampusAreSkipped() throws Exception {
        writeConfig("""
                grand_alliances:
                  concordat_of_the_dawn:
                    display_name: "Concordat of the Dawn"
                    chat_color: "&b"
                alignments:
                  good_alignment:
                    display_name: "Good"
                    grand_alliance: concordat_of_the_dawn
                    home_campus: taylors
                    enabled: true
                  bad_alliance:
                    display_name: "Bad Alliance"
                    grand_alliance: not_an_alliance
                    home_campus: taylors
                  bad_campus:
                    display_name: "Bad Campus"
                    grand_alliance: concordat_of_the_dawn
                    home_campus: not_a_campus
                """);

        AlignmentConfigManager manager = new AlignmentConfigManager(pluginFor(dataDirectory));
        manager.load();

        assertTrue(manager.getAlignment("good_alignment").isPresent());
        assertFalse(manager.getAlignment("bad_alliance").isPresent());
        assertFalse(manager.getAlignment("bad_campus").isPresent());
        assertEquals(1, manager.getAllAlignments().size());
    }

    @Test
    void duplicateIdsMissingFieldsAndDisabledAlignmentsAreHandled() throws Exception {
        writeConfig("""
                grand_alliances:
                  concordat_of_the_dawn:
                    display_name: "Concordat of the Dawn"
                  ironclad_syndicate:
                    display_name: "Ironclad Syndicate"
                alignments:
                  azure_hearth:
                    display_name: "Azure Hearth"
                    grand_alliance: concordat_of_the_dawn
                    home_campus: taylors
                  Azure_Hearth:
                    display_name: "Azure Hearth Impostor"
                    grand_alliance: concordat_of_the_dawn
                    home_campus: taylors
                  no_display_name:
                    grand_alliance: concordat_of_the_dawn
                    home_campus: taylors
                  retired_order:
                    display_name: "Retired Order"
                    grand_alliance: ironclad_syndicate
                    home_campus: monash
                    enabled: false
                """);

        AlignmentConfigManager manager = new AlignmentConfigManager(pluginFor(dataDirectory));
        manager.load();

        assertTrue(manager.getAlignment("azure_hearth").isPresent());
        assertEquals("Azure Hearth", manager.getAlignment("azure_hearth").get().displayName(),
                "case-insensitive duplicate ids must not overwrite the first definition");
        assertFalse(manager.getAlignment("no_display_name").isPresent(),
                "alignments missing display_name must be rejected");

        assertTrue(manager.getAlignment("retired_order").isPresent());
        assertFalse(manager.getAlignment("retired_order").get().enabled());
        assertEquals(List.of(), manager.getEnabledAlignments().stream()
                .filter(def -> def.id().equals("retired_order"))
                .toList());

        assertEquals("[Azure Hearth]", manager.getAlignment("azure_hearth").get().chatPrefix(),
                "missing chat_prefix should fall back to the display name");
    }

    private void writeConfig(String content) throws Exception {
        Files.writeString(dataDirectory.resolve("alignments.yml"), content);
    }

    private JavaPlugin pluginFor(Path directory) {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("AlignmentConfigManagerTest"));
        doAnswer(invocation -> {
            File out = new File(directory.toFile(), invocation.getArgument(0, String.class));
            if (!out.exists()) {
                try (InputStream in = getClass().getClassLoader().getResourceAsStream(out.getName())) {
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
