package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.VendorGameDeactivated;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorGameDeactivatedRepository extends JpaRepository<VendorGameDeactivated, Integer> {
    @Cacheable(value = "VendorGameDeactivated", key = "{#vendorGameId, #agentId}", cacheManager = "cacheManager")
    @Query(value = "SELECT * FROM vendor_game_deactivated " +
            "WHERE " +
            "(agent_id = :agentId OR master_agent_id = :masterAgentId OR house_id = :houseId OR sas_entity_hierarchy_id = :sasEntityHierarchyId) " +
            "AND " +
            "vendor_game_id = :vendorGameId " +
            "AND " +
            "is_deleted = :isDeleted " +
            "Limit 0,1", nativeQuery = true)
    VendorGameDeactivated findByVendorGameIdAndAgentIdAndAgentMasterIdAndHouseIdAndSasEntityHierarchyIdAndIsDeleted(@Param("vendorGameId") Integer vendorGameId, @Param("agentId") Integer agentId,
                                                                   @Param("masterAgentId") Integer masterAgentId, @Param("houseId") Integer houseId,
                                                                   @Param("isDeleted") Integer isDeleted, @Param("sasEntityHierarchyId") Integer sasEntityHierarchyId);

}