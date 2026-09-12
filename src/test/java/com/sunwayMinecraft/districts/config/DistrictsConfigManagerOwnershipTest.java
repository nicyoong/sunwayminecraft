package com.sunwayMinecraft.districts.config;

import com.sunwayMinecraft.districts.domain.DistrictDefinition;
import com.sunwayMinecraft.districts.domain.DistrictType;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Loading and validation of the extended districts.yml schema. */
class DistrictsConfigManagerOwnershipTest {
    @TempDir
    Path dataDirectory;

    private DistrictsConfigManager configFor(String yaml, java.util.function.Predicate<String> knownAlignment) {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(dataDirectory.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("DistrictsOwnershipTest"));
        doAnswer(invocation -> {
            // only copy bundled defaults when the test did not write its own file
            if (yaml == null) {
                File out = new File(dataDirectory.toFile(), invocation.getArgument(0, String.class));
                if (!out.exists()) {
                    try (InputStream in = getClass().getClassLoader().getResourceAsStream(out.getName())) {
                        if (in != null) {
                            Files.copy(in, out.toPath());
                        }
                    }
                }
            }
            return null;
        }).when(plugin).saveResource(anyString(), anyBoolean());
        if (yaml != null) {
            try {
                Files.writeString(dataDirectory.resolve("districts.yml"), yaml);
            } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
            }
        }
        DistrictsConfigManager manager = new DistrictsConfigManager(plugin, knownAlignment);
        manager.reload();
        return manager;
    }

    private static final java.util.function.Predicate<String> KNOWS_ALLIANCE_IDS =
            id -> Set.of("azure_hearth", "spirewrights", "concordat_of_the_dawn", "ironclad_syndicate")
                    .contains(id);

    @Test
    void defaultResourceLoadsEightTripleAllianceDistrictsBackwardCompatibly() {
        DistrictsConfigManager manager = configFor(null, KNOWS_ALLIANCE_IDS);

        assertEquals(9, manager.getDistricts().size(),
                "legacy central_residency plus the eight alliance districts");
        assertEquals("residential_standard",
                manager.getDistrictById("central_residency").getOwnership().propertyPolicy(),
                "the legacy policy-profile key must feed the property policy field");

        assertEquals(DistrictType.CAMPUS,
                manager.getDistrictById("taylors_lakeside_campus").getDistrictType());
        assertEquals("taylors",
                manager.getDistrictById("taylors_lakeside_campus").getOwnership().homeCampus());
        assertEquals(DistrictType.ARCHIVED,
                manager.getDistrictById("old_world_archive").getDistrictType());
        assertTrue(manager.getDistrictById("old_world_archive").isArchived());
        assertTrue(manager.getDistrictById("brt_central_transit").getOwnership().transitConnected());
        assertTrue(manager.getDistrictById("triple_alliance_sanctuary").getOwnership().isNeutral(),
                "shipped defaults are neutral");
    }

    @Test
    void specGettersGroupDistrictsByCampusAllianceTypeContestAndTransit() throws Exception {
        DistrictsConfigManager manager = configFor("""
                districts:
                  campus_a:
                    display-name: "Campus A"
                    district-type: CAMPUS
                    home_campus: taylors
                    grand_alliance_owner: concordat_of_the_dawn
                    world: "world"
                    region: { min: {x: 0, y: 0, z: 0}, max: {x: 9, y: 9, z: 9} }
                  pyramids:
                    display-name: "Pyramids"
                    district-type: COMMERCIAL
                    home_campus: sunway
                    transit_connected: true
                    contested: true
                    world: "world"
                    region: { min: {x: 20, y: 0, z: 0}, max: {x: 29, y: 9, z: 9} }
                """, KNOWS_ALLIANCE_IDS);

        assertEquals(1, manager.getDistrictsByCampus("sunway").size());
        assertEquals(1, manager.getDistrictsByGrandAlliance("concordat_of_the_dawn").size());
        assertEquals(1, manager.getDistrictsByType(DistrictType.CAMPUS).size());
        assertEquals(1, manager.getContestedDistricts().size());
        assertEquals(1, manager.getTransitDistricts().size());
        assertEquals(2, manager.getEnabledDistricts().size());
    }

    @Test
    void ownershipFieldsParseInBothSpellingsWithDeniedTakingPriority() throws Exception {
        configFor("""
                districts:
                  mixed:
                    display-name: "Mixed"
                    district-type: MIXED_USE
                    home-campus: taylors
                    grand-alliance-owner: ironclad_syndicate
                    alignment_owner: spirewrights
                    allowed_alignments: [azure_hearth, spirewrights]
                    denied_alignments: [spirewrights]
                    property_policy: "residential_standard"
                    transit-connected: true
                    world: "world"
                    region: { min: {x: 0, y: 0, z: 0}, max: {x: 9, y: 9, z: 9} }
                """, KNOWS_ALLIANCE_IDS);
        DistrictsConfigManager manager = configFor(null, KNOWS_ALLIANCE_IDS);

        DistrictDefinition district = manager.getDistrictById("mixed");
        assertEquals("taylors", district.getOwnership().homeCampus());
        assertEquals("ironclad_syndicate", district.getOwnership().grandAllianceOwner());
        assertEquals("spirewrights", district.getOwnership().alignmentOwner());
        assertEquals("residential_standard", district.getOwnership().propertyPolicy());
        assertTrue(district.getOwnership().transitConnected());
        assertFalse(district.getAccessRule().allows("spirewrights"),
                "denied must win even when also allowed");
        assertTrue(district.getAccessRule().allows("azure_hearth"));
    }

    @Test
    void unknownAlignmentReferencesDegradeToNeutralWithWarning() throws Exception {
        configFor("""
                districts:
                  claimed:
                    display-name: "Claimed"
                    district-type: CAMPUS
                    home_campus: monash
                    grand_alliance_owner: not_an_alliance
                    alignment_owner: ghost_alignment
                    allowed_alignments: [missing_alignment]
                    world: "world"
                    region: { min: {x: 0, y: 0, z: 0}, max: {x: 9, y: 9, z: 9} }
                """, KNOWS_ALLIANCE_IDS);
        DistrictsConfigManager manager = configFor(null, KNOWS_ALLIANCE_IDS);

        DistrictDefinition district = manager.getDistrictById("claimed");
        assertNull(district.getOwnership().grandAllianceOwner(),
                "unknown owners must not survive validation");
        assertNull(district.getOwnership().alignmentOwner());
        assertTrue(district.getOwnership().allowedAlignments().isEmpty());
        assertTrue(district.getOwnership().isNeutral(),
                "the district itself must survive with neutral ownership");
        assertEquals("monash", district.getOwnership().homeCampus(),
                "campus is not alignment-validated and stays");
    }

    @Test
    void duplicateIdsAndInvalidTypesAreSkippedWithWarnings() throws Exception {
        // exact duplicate keys are collapsed by the YAML parser itself, so the
        // manager-level duplicate check is exercised with a case-variant pair
        configFor("""
                districts:
                  Duplicated:
                    display-name: "First"
                    district-type: CAMPUS
                    world: "world"
                    region: { min: {x: 0, y: 0, z: 0}, max: {x: 9, y: 9, z: 9} }
                  duplicated:
                    display-name: "Second"
                    district-type: CAMPUS
                    world: "world"
                    region: { min: {x: 20, y: 0, z: 0}, max: {x: 29, y: 9, z: 9} }
                  broken_type:
                    display-name: "Broken"
                    district-type: NOT_A_TYPE
                    world: "world"
                    region: { min: {x: 40, y: 0, z: 0}, max: {x: 49, y: 9, z: 9} }
                  good:
                    display-name: "Good"
                    district-type: CIVIC
                    world: "world"
                    region: { min: {x: 60, y: 0, z: 0}, max: {x: 69, y: 9, z: 9} }
                """, KNOWS_ALLIANCE_IDS);
        DistrictsConfigManager manager = configFor(null, KNOWS_ALLIANCE_IDS);

        assertEquals("First", manager.getDistrictById("duplicated").getDisplayName(),
                "the first entry must win on case-insensitive duplicate ids");
        assertFalse(manager.getDistricts().stream()
                .anyMatch(d -> d.getId().equals("broken_type")),
                "invalid district types must skip the entry instead of aborting the reload");
        assertTrue(manager.getDistrictById("good") != null,
                "valid entries after broken ones must still load");
    }

    @Test
    void propertyPolicyResolutionFallsBackToDefaultWhenPolicyIsMissing() throws Exception {
        configFor("""
                districts:
                  linked:
                    display-name: "Linked"
                    district-type: RESIDENTIAL
                    property_policy: "residential_standard"
                    world: "world"
                    region: { min: {x: 0, y: 0, z: 0}, max: {x: 9, y: 9, z: 9} }
                  broken_link:
                    display-name: "Broken Link"
                    district-type: RESIDENTIAL
                    policy-profile: "no_such_policy"
                    world: "world"
                    region: { min: {x: 20, y: 0, z: 0}, max: {x: 29, y: 9, z: 9} }
                  unlinked:
                    display-name: "Unlinked"
                    district-type: RESIDENTIAL
                    world: "world"
                    region: { min: {x: 40, y: 0, z: 0}, max: {x: 49, y: 9, z: 9} }
                """, KNOWS_ALLIANCE_IDS);

        // give the resolver a real policy file so the strict path is exercised
        Files.writeString(dataDirectory.resolve("property-policies.yml"), """
                policy-profiles:
                  residential_standard: {}
                  commercial_standard: {}
                """);
        DistrictsConfigManager manager = configFor(null, KNOWS_ALLIANCE_IDS);

        assertEquals("residential_standard",
                manager.resolvePropertyPolicyId(manager.getDistrictById("linked")));
        assertEquals("default",
                manager.resolvePropertyPolicyId(manager.getDistrictById("broken_link")));
        assertEquals("default",
                manager.resolvePropertyPolicyId(manager.getDistrictById("unlinked")),
                "districts without a reference use the default policy");
    }
}
