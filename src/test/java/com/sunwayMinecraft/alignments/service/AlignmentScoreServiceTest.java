package com.sunwayMinecraft.alignments.service;

import com.sunwayMinecraft.alignments.config.AlignmentConfigManager;
import com.sunwayMinecraft.alignments.config.AlignmentProgressionConfig;
import com.sunwayMinecraft.alignments.domain.AlignmentDefinition;
import com.sunwayMinecraft.alignments.domain.Campus;
import com.sunwayMinecraft.alignments.domain.GrandAlliance;
import com.sunwayMinecraft.alignments.persistence.AlignmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AlignmentScoreServiceTest {
    private AlignmentProgressionConfig progressionConfig;
    private AlignmentConfigManager configManager;
    private AlignmentRepository membershipRepository;
    private AlignmentScoreService service;

    private final AlignmentDefinition azureHearth = new AlignmentDefinition(
            "azure_hearth", "Azure Hearth", GrandAlliance.CONCORDAT_OF_THE_DAWN,
            Campus.TAYLORS, "desc", "[Azure Hearth]", "&9", true);
    private final AlignmentDefinition spirewrights = new AlignmentDefinition(
            "spirewrights", "Spirewrights", GrandAlliance.IRONCLAD_SYNDICATE,
            Campus.TAYLORS, "desc", "[Spirewrights]", "&e", true);

    @BeforeEach
    void setUp() {
        progressionConfig = mock(AlignmentProgressionConfig.class);
        when(progressionConfig.getScoreWeights())
                .thenReturn(new AlignmentProgressionConfig.ScoreWeights(1, 2, 25, 1));
        configManager = mock(AlignmentConfigManager.class);
        when(configManager.getAlignment("azure_hearth")).thenReturn(Optional.of(azureHearth));
        when(configManager.getAlignment("spirewrights")).thenReturn(Optional.of(spirewrights));
        when(configManager.getAlignment("unknown_alignment")).thenReturn(Optional.empty());
        membershipRepository = mock(AlignmentRepository.class);
        service = new AlignmentScoreService(progressionConfig, configManager, membershipRepository);
    }

    @Test
    void scoreCombinesReputationAndMemberCountsWithConfiguredWeights() {
        when(membershipRepository.getAlignmentTotals()).thenReturn(Map.of(
                "azure_hearth", new long[] {400, 10},
                "spirewrights", new long[] {100, 4}));

        // concordat: 1 * 400 + 2 * 10 = 420; ironclad: 1 * 100 + 2 * 4 = 108
        assertEquals(420L, service.getGrandAllianceScore("concordat_of_the_dawn"));
        assertEquals(108L, service.getGrandAllianceScore("ironclad_syndicate"));
    }

    @Test
    void alignmentsWithoutConfigurationAreIgnored() {
        when(membershipRepository.getAlignmentTotals()).thenReturn(Map.of(
                "unknown_alignment", new long[] {9999, 99},
                "azure_hearth", new long[] {50, 2}));

        assertEquals(54L, service.getGrandAllianceScore("concordat_of_the_dawn"),
                "memberships with vanished alignments must not affect any score");
    }

    @Test
    void optionalSystemsContributeZeroUntilTheyProvideData() {
        when(membershipRepository.getAlignmentTotals()).thenReturn(Map.of());
        assertEquals(0L, service.getGrandAllianceScore("concordat_of_the_dawn"),
                "district and contract weights contribute nothing without per-alliance data");
    }

    @Test
    void futureDistrictAndContractDataCanBeInjectedViaOverrides() {
        AlignmentScoreService withData = new AlignmentScoreService(
                progressionConfig, configManager, membershipRepository) {
            @Override
            protected int districtsHeld(String grandAllianceId) {
                return "concordat_of_the_dawn".equals(grandAllianceId) ? 2 : 0;
            }

            @Override
            protected int contractsCompleted(String grandAllianceId) {
                return "concordat_of_the_dawn".equals(grandAllianceId) ? 5 : 0;
            }
        };
        when(membershipRepository.getAlignmentTotals())
                .thenReturn(Map.of("azure_hearth", new long[] {0, 0}));

        // 25 * 2 districts + 1 * 5 contracts = 55
        assertEquals(55L, withData.getGrandAllianceScore("concordat_of_the_dawn"));
    }

    @Test
    void allAllianceScoresAreReportedAtOnce() {
        when(membershipRepository.getAlignmentTotals()).thenReturn(Map.of(
                "azure_hearth", new long[] {10, 1},
                "spirewrights", new long[] {20, 2}));

        Map<String, Long> scores = service.getGrandAllianceScores();
        assertEquals(2, scores.size());
        assertEquals(12L, scores.get("concordat_of_the_dawn"));
        assertEquals(24L, scores.get("ironclad_syndicate"));
        assertTrue(scores.values().stream().allMatch(score -> score >= 0));
    }
}
