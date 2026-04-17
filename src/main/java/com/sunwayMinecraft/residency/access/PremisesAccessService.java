package com.sunwayMinecraft.residency.access;

import com.sunwayMinecraft.residency.ResidencyManager;
import com.sunwayMinecraft.residency.domain.*;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.UUID;

public class PremisesAccessService {
    private final ResidencyManager manager;

    public PremisesAccessService(ResidencyManager manager) { this.manager = manager; }

    public AccessDecision check(Player player, Location location, ActionType action) {
        UnitDefinition unit = manager.getUnitAt(location);
        if (unit == null) return AccessDecision.allow(null, null);
        if (player.hasPermission("sunway.residency.override") || player.hasPermission("sunway.residency.staff")) {
            return AccessDecision.allow(unit, RoleType.STAFF_OVERRIDE);
        }
        UnitTenancyRecord tenancy = manager.getRepository().getTenancy(unit.getId());
        UUID playerId = player.getUniqueId();
        RoleType role = resolveRole(unit.getId(), tenancy, playerId, Instant.now());
        PolicyProfile policy = manager.getPolicyProfile(unit);

        if (tenancy.getLeaseState() == LeaseState.REPOSSESSED || tenancy.getLeaseState() == LeaseState.ESCROW_OPEN) {
            return AccessDecision.deny(unit, role, "This unit has been repossessed.");
        }
        if (tenancy.getLeaseState() == LeaseState.ARREARS_RESTRICTED && role != RoleType.TENANT && role != RoleType.MANAGER) {
            return AccessDecision.deny(unit, role, "This unit is restricted.");
        }
        if (role == RoleType.TENANT || role == RoleType.OWNER || role == RoleType.STAFF_OVERRIDE) {
            return AccessDecision.allow(unit, role);
        }
        if (role == RoleType.MANAGER) {
            if (action == ActionType.BREAK_BLOCK || action == ActionType.PLACE_BLOCK) {
                return AccessDecision.deny(unit, role, "Managers may not edit blocks by default.");
            }
            return AccessDecision.allow(unit, role);
        }
        if (role == RoleType.GUEST) {
            return switch (action) {
                case ENTER, OPEN_DOOR, USE_BUTTON_OR_LEVER, INTERACT_ENTITY -> AccessDecision.allow(unit, role);
                default -> AccessDecision.deny(unit, role, "Guests may not do that here.");
            };
        }
        return switch (action) {
            case ENTER, OPEN_DOOR -> unit.getFlags().isPublicEntry() ? AccessDecision.allow(unit, null) : AccessDecision.deny(unit, null, "This premises is not publicly accessible.");
            case USE_CONTAINER -> (unit.getFlags().isPublicContainerAccess() || policy.isAllowPublicContainerAccess()) ? AccessDecision.allow(unit, null) : AccessDecision.deny(unit, null, "Public container access is not allowed here.");
            case USE_BUTTON_OR_LEVER, INTERACT_ENTITY -> (unit.getFlags().isPublicInteraction() || policy.isAllowPublicInteraction()) ? AccessDecision.allow(unit, null) : AccessDecision.deny(unit, null, "Public interaction is not allowed here.");
            default -> AccessDecision.deny(unit, null, "You do not have access to this premises.");
        };
    }

    private RoleType resolveRole(String unitId, UnitTenancyRecord tenancy, UUID playerId, Instant now) {
        if (tenancy.getTenantPlayerId() != null && tenancy.getTenantPlayerId().equals(playerId)) return RoleType.TENANT;
        if (tenancy.getManagerIds().contains(playerId)) return RoleType.MANAGER;
        for (RoleAssignment assignment : manager.getRepository().getRoleAssignments(unitId)) {
            if (assignment.getPlayerId().equals(playerId) && !assignment.isExpired(now)) return assignment.getRoleType();
        }
        for (GuestAccessGrant grant : manager.getRepository().getGuestAccess(unitId)) {
            if (grant.getPlayerId().equals(playerId) && grant.isActive(now)) return RoleType.GUEST;
        }
        return null;
    }
}
