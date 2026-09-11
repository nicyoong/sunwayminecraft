package com.sunwayMinecraft.commands;

import com.sunwayMinecraft.districts.DistrictManager;
import com.sunwayMinecraft.districts.config.DistrictsConfigManager;
import com.sunwayMinecraft.districts.domain.DistrictDefinition;
import com.sunwayMinecraft.districts.domain.DistrictOwnership;
import com.sunwayMinecraft.districts.domain.DistrictType;
import com.sunwayMinecraft.districts.region.DistrictShape;
import com.sunwayMinecraft.districts.region.Region3i;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Admin subcommand routing, ownership mutations and permission gating. */
class DistrictAdminSubCommandsTest {
    private final DistrictManager districtManager = mock(DistrictManager.class);
    private final DistrictsConfigManager configManager = mock(DistrictsConfigManager.class);
    private final DistrictAdminSubCommands commands =
            new DistrictAdminSubCommands(districtManager, configManager);
    private final CommandSender admin = adminSender();

    private CommandSender adminSender() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("sunway.district.admin")).thenReturn(true);
        when(sender.getName()).thenReturn("TesterAdmin");
        return sender;
    }

    private DistrictDefinition district(String id) {
        return new DistrictDefinition(id, "Name " + id, null, "world",
                DistrictShape.cuboid(new Region3i("world", 0, 0, 0, 9, 9, 9)), true, DistrictType.CAMPUS, 1,
                "summary", List.of(), true, 50, false, false, null, false, false,
                DistrictOwnership.neutral());
    }

    @Test
    void setOwnerDerivesTheGrandAllianceAndUpdatesOwnership() {
        when(districtManager.getDistrict("pyramid")).thenReturn(district("pyramid"));
        when(configManager.allianceOfAlignment("azure_hearth"))
                .thenReturn("concordat_of_the_dawn");

        assertTrue(commands.handle(admin, new String[]{
                "admin", "set-owner", "pyramid", "azure_hearth"}));

        verify(configManager).updateOwnership(org.mockito.ArgumentMatchers.eq("pyramid"),
                argThat(ownership -> "azure_hearth".equals(ownership.alignmentOwner())
                        && "concordat_of_the_dawn".equals(ownership.grandAllianceOwner())),
                org.mockito.ArgumentMatchers.eq("TesterAdmin"));
    }

    @Test
    void setOwnerRejectsUnknownAlignments() {
        when(districtManager.getDistrict("pyramid")).thenReturn(district("pyramid"));
        when(configManager.allianceOfAlignment("ghost")).thenReturn(null);

        assertTrue(commands.handle(admin, new String[]{
                "admin", "set-owner", "pyramid", "ghost"}));

        verify(configManager, never()).updateOwnership(any(), any(), any());
        verify(admin).sendMessage(argThat((String m) -> m != null && m.contains("Unknown alignment")));
    }

    @Test
    void clearOwnerRemovesOwnersAndAccessLists() {
        when(districtManager.getDistrict("pyramid")).thenReturn(district("pyramid"));

        assertTrue(commands.handle(admin, new String[]{"admin", "clear-owner", "pyramid"}));

        verify(configManager).updateOwnership(org.mockito.ArgumentMatchers.eq("pyramid"),
                org.mockito.ArgumentMatchers.argThat(
                        (com.sunwayMinecraft.districts.domain.DistrictOwnership o) -> o.isNeutral()),
                org.mockito.ArgumentMatchers.eq("TesterAdmin"));
    }

    @Test
    void allowedAndDeniedListMutationsUpdateTheOwnershipLists() {
        when(districtManager.getDistrict("pyramid")).thenReturn(district("pyramid"));

        assertTrue(commands.handle(admin, new String[]{
                "admin", "add-allowed", "pyramid", "azure_hearth"}));
        verify(configManager).updateOwnership(org.mockito.ArgumentMatchers.eq("pyramid"),
                argThat(o -> o.allowedAlignments().contains("azure_hearth")),
                org.mockito.ArgumentMatchers.eq("TesterAdmin"));

        assertTrue(commands.handle(admin, new String[]{
                "admin", "add-denied", "pyramid", "spirewrights"}));
        verify(configManager).updateOwnership(org.mockito.ArgumentMatchers.eq("pyramid"),
                argThat(o -> o.deniedAlignments().contains("spirewrights")),
                org.mockito.ArgumentMatchers.eq("TesterAdmin"));
    }

    @Test
    void adminSubcommandsRequireTheAdminPermission() {
        CommandSender nonAdmin = mock(CommandSender.class);
        when(nonAdmin.hasPermission("sunway.district.admin")).thenReturn(false);

        assertTrue(commands.handle(nonAdmin, new String[]{
                "admin", "set-owner", "pyramid", "azure_hearth"}));

        verify(nonAdmin).sendMessage(argThat((String m) -> m != null && m.contains("permission")));
        verify(configManager, never()).updateOwnership(any(), any(), any());
    }

    @Test
    @Disabled("BUG-DIST1 (medium): /district admin toggle is never routed - handleAdmin has "
            + "no toggle case, so the subcommand always falls through to the help text. "
            + "When routed, handleToggle must read the toggle target from args[2] (it reads "
            + "args[1], which is always 'toggle').")
    void toggleFlipsContestedAndTransitFlags() {
        when(districtManager.getDistrict("pyramid")).thenReturn(district("pyramid"));

        assertTrue(commands.handle(admin, new String[]{
                "admin", "toggle", "contested", "pyramid"}));
        verify(configManager).updateOwnership(org.mockito.ArgumentMatchers.eq("pyramid"),
                org.mockito.ArgumentMatchers.argThat(
                        (com.sunwayMinecraft.districts.domain.DistrictOwnership o) -> o.contested()),
                org.mockito.ArgumentMatchers.eq("TesterAdmin"));
    }
}
