package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.Vendor;
import com.nextgen.gameaggregator.entity.custom.IGameVendor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, Integer> {
    Vendor findByCode(String code);

    Vendor findVendorById(Integer id);

//    @Query(value=" SELECT " +
//            " vendors.code, vendors.name  FROM vendors WHERE vendors.id IN " +
//            "(SELECT vendor_id FROM agent_vendor_lines WHERE agent_vendor_lines.status =:status AND agent_vendor_lines.agent_id =:agentId GROUP BY agent_vendor_lines.vendor_id) " +
//            "ORDER BY vendors.code", nativeQuery=true)

    @Query(value=" SELECT " +
            "v.name, v.code , GROUP_CONCAT(DISTINCT gc.code SEPARATOR ',') AS categoryCode " +
            "FROM agent_vendor_lines avl " +
            "INNER JOIN game_categories gc on avl.game_category_id = gc.id " +
            "INNER JOIN vendors v on avl.vendor_id = v.id "+
            "WHERE avl.status =:status AND avl.agent_id =:agentId group by avl.vendor_id ", nativeQuery=true)
    List<IGameVendor> findByAgentSupportedVendorAndStatus (@Param("agentId") int agentId, @Param("status") int status);


}

