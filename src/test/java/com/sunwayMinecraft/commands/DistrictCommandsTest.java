package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.districts.DistrictManager;
import com.sunwayMinecraft.districts.domain.DistrictDefinition;
import com.sunwayMinecraft.districts.domain.DistrictOwnership;
import com.sunwayMinecraft.districts.domain.DistrictType;
import com.sunwayMinecraft.districts.region.DistrictShape;
import com.sunwayMinecraft.districts.region.Region3i;
import com.sunwayMinecraft.districts.service.DistrictAlignmentService;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** /district output: alliance fields, access status, grouping and pagination. */
class DistrictCommandsTest {
    private ServerMock server;
    private PlayerMock player;
    private DistrictManager districtManager;
    private DistrictCommands commands;

    private static final DistrictShape REGION = DistrictShape.cuboid(new Region3i("world", 0, 0, 0, 9, 9, 9));

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        player = server.addPlayer();
        player.setOp(true); // sunway.district.use defaults to op-independent true, ops always pass
        districtManager = mock(DistrictManager.class);
        DistrictAlignmentService service = new DistrictAlignmentService(
                mock(com.sunwayMinecraft.districts.config.DistrictsConfigManager.class),
                uuid -> Optional.empty());
        commands = new DistrictCommands(districtManager, service,
                new com.sunwayMinecraft.commands.DistrictAdminSubCommands(districtManager,
                        mock(com.sunwayMinecraft.districts.config.DistrictsConfigManager.class)));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private DistrictDefinition districtAt(String id, DistrictType type, String campus,
                                          DistrictOwnership ownership) {
        return new DistrictDefinition(id, "Name " + id, null, "world", REGION, true,
                type, 2, "summary", List.of(), true, 50, false, false, null, false, false,
                ownership);
    }

    private final DistrictOwnership sunwayOwned = new DistrictOwnership(
            "sunway", "concordat_of_the_dawn", "lagoon_covenant", List.of(), List.of(),
            null, true, false);
    private final DistrictOwnership contested = new DistrictOwnership(
            "taylors", null, null, List.of(), List.of(), null, false, true);

    @Test
    void currentDistrictShowsCampusOwnerTransitAndAccessStatus() {
        DistrictDefinition district = districtAt(
                "sunway_university_campus", DistrictType.CAMPUS, "sunway", sunwayOwned);
        when(districtManager.getDistrictAt(any(Location.class))).thenReturn(district);

        assertTrue(commands.onCommand(player, command("district"), "district", new String[0]));

        List<String> messages = drainMessages();
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("Campus:")),
                "campus line expected, got: " + messages);
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("sunway")),
                "campus id expected, got: " + messages);
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("lagoon_covenant")),
                "owner alignment expected, got: " + messages);
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("Transit:")), messages.toString());
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("Access:")),
                "personal access status expected, got: " + messages);
    }

    @Test
    void neutralDistrictShowsNeutralOwnerAndContestedFlag() {
        when(districtManager.getDistrictAt(any(Location.class)))
                .thenReturn(districtAt("sanctuary", DistrictType.SANCTUARY, null, contested));

        assertTrue(commands.onCommand(player, command("district"), "district", new String[0]));

        List<String> messages = drainMessages();
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("Neutral")), messages.toString());
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("CONTESTED")), messages.toString());
    }

    @Test
    void infoShowsAllianceDetailsForNamedDistricts() {
        when(districtManager.getDistrict("old_world_archive"))
                .thenReturn(districtAt("old_world_archive", DistrictType.ARCHIVED, null,
                        DistrictOwnership.neutral()));

        assertTrue(commands.onCommand(player, command("district"), "district",
                new String[]{"info", "old_world_archive"}));

        List<String> messages = drainMessages();
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("Owner:")), messages.toString());
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("Neutral")), messages.toString());
    }

    @Test
    void listGroupsByCampusAndPaginates() {
        List<DistrictDefinition> districts = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            districts.add(districtAt("district_" + i, DistrictType.CAMPUS,
                    i <= 4 ? "taylors" : "monash", DistrictOwnership.neutral()));
        }
        when(districtManager.getPublicDistricts()).thenReturn(districts);

        assertTrue(commands.onCommand(player, command("district"), "district",
                new String[]{"list", "type", "2"}));

        List<String> messages = drainMessages();
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("page 2/2")), messages.toString());
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("district_9")),
                "the last district must appear on page 2");
        assertTrue(messages.stream().noneMatch(msg -> msg.contains("district_1 ")),
                "the first district must remain on page 1");
    }

    private Command command(String name) {
        Command command = mock(Command.class);
        when(command.getName()).thenReturn(name);
        return command;
    }

    private List<String> drainMessages() {
        List<String> messages = new ArrayList<>();
        String message;
        while ((message = player.nextMessage()) != null) {
            messages.add(message);
        }
        return messages;
    }
}
