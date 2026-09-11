package com.sunwayMinecraft.alignments.config;

import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AlignmentSettingsConfigTest {
    @TempDir
    Path dataDirectory;

    @Test
    void missingSettingsFileIsGeneratedAndDefaultsAreSafe() {
        AlignmentSettingsConfig settings = new AlignmentSettingsConfig(pluginFor(dataDirectory));
        settings.load();

        assertTrue(Files.exists(dataDirectory.resolve("alignments-settings.yml")),
                "missing settings file must be regenerated from the default resource");
        assertTrue(settings.isAllowAlignmentChat());
        assertTrue(settings.isAllowGlobalChatPrefix());
        assertEquals(0L, settings.getSwitchCooldownSeconds(),
                "cooldown must default to disabled");
        assertFalse(settings.isCooldownAppliesToLeave());
        assertTrue(settings.isAllowAdminBypass());
        assertTrue(settings.isShowJoinMessage());
        assertTrue(settings.isShowLeaveMessage());
        assertEquals(AlignmentSettingsConfig.DisabledPrefixMode.HIDE, settings.getDisabledPrefixMode());
        assertEquals(AlignmentSettingsConfig.StaleMembershipAction.KEEP,
                settings.getStaleMembershipAction());
        assertTrue(settings.getChatPrefixFormat().contains("{grand_alliance_short}"));
        assertTrue(settings.getAlignmentChatFormat().contains("{message}"));
    }

    @Test
    void customValuesAreLoadedFromTheSettingsFile() throws Exception {
        Files.writeString(dataDirectory.resolve("alignments-settings.yml"), """
                allow_alignment_chat: false
                allow_global_chat_prefix: false
                chat_prefix_format: "<{alignment}> "
                alignment_chat_format: "{player} says {message}"
                alignment_chat_color: "&d"
                switch_cooldown_seconds: 300
                cooldown_applies_to_leave: true
                allow_admin_bypass: false
                show_join_message: false
                show_leave_message: false
                disabled_alignment_prefix_mode: neutral
                stale_membership_action: unalign
                """);

        AlignmentSettingsConfig settings = new AlignmentSettingsConfig(pluginFor(dataDirectory));
        settings.load();

        assertFalse(settings.isAllowAlignmentChat());
        assertFalse(settings.isAllowGlobalChatPrefix());
        assertEquals("<{alignment}> ", settings.getChatPrefixFormat());
        assertEquals("{player} says {message}", settings.getAlignmentChatFormat());
        assertEquals("&d", settings.getAlignmentChatColor());
        assertEquals(300L, settings.getSwitchCooldownSeconds());
        assertTrue(settings.isCooldownAppliesToLeave());
        assertFalse(settings.isAllowAdminBypass());
        assertFalse(settings.isShowJoinMessage());
        assertFalse(settings.isShowLeaveMessage());
        assertEquals(AlignmentSettingsConfig.DisabledPrefixMode.NEUTRAL, settings.getDisabledPrefixMode());
        assertEquals(AlignmentSettingsConfig.StaleMembershipAction.UNALIGN,
                settings.getStaleMembershipAction());
    }

    @Test
    void invalidEnumValuesFallBackToSafeDefaults() throws Exception {
        Files.writeString(dataDirectory.resolve("alignments-settings.yml"), """
                disabled_alignment_prefix_mode: explosion
                stale_membership_action: maybe
                """);

        AlignmentSettingsConfig settings = new AlignmentSettingsConfig(pluginFor(dataDirectory));
        settings.load();

        assertEquals(AlignmentSettingsConfig.DisabledPrefixMode.HIDE, settings.getDisabledPrefixMode());
        assertEquals(AlignmentSettingsConfig.StaleMembershipAction.KEEP,
                settings.getStaleMembershipAction());
    }

    @Test
    void negativeCooldownIsClampedToZero() throws Exception {
        Files.writeString(dataDirectory.resolve("alignments-settings.yml"),
                "switch_cooldown_seconds: -50\n");
        AlignmentSettingsConfig settings = new AlignmentSettingsConfig(pluginFor(dataDirectory));
        settings.load();
        assertEquals(0L, settings.getSwitchCooldownSeconds());
    }

    private JavaPlugin pluginFor(Path directory) {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("AlignmentSettingsConfigTest"));
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
