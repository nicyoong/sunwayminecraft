package com.sunwayMinecraft.residency.storage;

import com.sunwayMinecraft.residency.domain.*;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ResidencyRepository {
    void load();
    void save();

    UnitTenancyRecord getTenancy(String unitId);
    void saveTenancy(UnitTenancyRecord record);
    Collection<UnitTenancyRecord> getAllTenancies();

    List<RoleAssignment> getRoleAssignments(String unitId);
    void addRoleAssignment(RoleAssignment assignment);
    void removeRoleAssignments(String unitId, UUID playerId, RoleType roleType);

    List<GuestAccessGrant> getGuestAccess(String unitId);
    void addGuestAccess(GuestAccessGrant grant);
    void purgeExpiredGuestAccess();

    EscrowRecord getEscrow(String unitId);
    void saveEscrow(EscrowRecord record);
}

