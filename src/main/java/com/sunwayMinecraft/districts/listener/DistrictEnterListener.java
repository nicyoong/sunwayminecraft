package com.sunwayMinecraft.districts.listener;

import com.sunwayMinecraft.districts.config.DistrictSettingsConfig;
import com.sunwayMinecraft.districts.domain.DistrictDefinition;
import com.sunwayMinecraft.districts.region.DistrictLocationResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shows a chat or action-bar message when a player enters a different
 * district. Spam is prevented two ways: only block-coordinate changes are
 * evaluated, and the same district is never re-sent until the configured
 * cooldown elapses (or forever, when the cooldown is 0).
 */
public class DistrictEnterListener implements Listener {
    private final JavaPlugin plugin;
    private final DistrictLocationResolver resolver;
    private final DistrictSettingsConfig settings;
    private final Map<UUID, String> currentDistrict = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastShownAt = new ConcurrentHashMap<>();

    public DistrictEnterListener(
            JavaPlugin plugin,
            DistrictLocationResolver resolver,
            DistrictSettingsConfig settings) {
        this.plugin = plugin;
        this.resolver = resolver;
        this.settings = settings;
    }

    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        // cheap early-out: only block changes can change the district
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()) {
            return;
        }
        handleLocation(event.getPlayer(), event.getTo());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        handleLocation(event.getPlayer(), event.getPlayer().getLocation());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        currentDistrict.remove(event.getPlayer().getUniqueId());
        lastShownAt.remove(event.getPlayer().getUniqueId());
    }

    /** Visible for testing. */
    public void handleLocation(Player player, org.bukkit.Location location) {
        DistrictDefinition district = resolver.getDistrictAt(location);
        String districtId = district == null ? null : district.getId();
        UUID playerUuid = player.getUniqueId();

        String previous = currentDistrict.put(playerUuid, districtId);
        long now = System.currentTimeMillis();
        boolean sameDistrict = districtId != null && districtId.equals(previous);
        if (sameDistrict) {
            long cooldownMs = settings.getEnterMessageCooldownSeconds() * 1000L;
            Long shown = lastShownAt.get(playerUuid);
            if (cooldownMs <= 0 || (shown != null && now - shown < cooldownMs)) {
                return;
            }
            if (shown == null) {
                return; // just entered; already announced below
            }
        }
        lastShownAt.put(playerUuid, now);

        if (district != null) {
            String message = buildMessage(district);
            switch (settings.getEnterMessageMode()) {
                case CHAT -> player.sendMessage(Component.text(message, NamedTextColor.GOLD));
                case ACTION_BAR -> player.sendActionBar(Component.text(message, NamedTextColor.GOLD));
                case NONE -> { }
            }
        }
    }

    private String buildMessage(DistrictDefinition district) {
        StringBuilder message = new StringBuilder(district.getDisplayName());
        if (settings.isShowDistrictType()) {
            message.append(" — ").append(district.getDistrictType().name().toLowerCase()
                    .replace('_', ' '));
        }
        if (settings.isShowOwner()) {
            String owner = district.getOwnership().alignmentOwner() != null
                    ? district.getOwnership().alignmentOwner()
                    : district.getOwnership().grandAllianceOwner();
            message.append(owner != null ? " — " + owner + " Territory" : " — Neutral Ground");
        }
        if (settings.isShowContestedStatus() && district.getOwnership().contested()) {
            message.append(" (CONTESTED)");
        }
        return message.toString();
    }
}
