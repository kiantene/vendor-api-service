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

    @Query(value=" SELECT " +
            "v.code , GROUP_CONCAT(DISTINCT gc.code SEPARATOR ',') AS categoryCode, " +
            "IFNULL(vlc.name , v.name) AS name " +
            "FROM agent_vendor_lines avl " +
            "INNER JOIN game_categories gc ON avl.game_category_id = gc.id " +
            "INNER JOIN vendors v ON avl.vendor_id = v.id " +
            "LEFT JOIN vendor_language_codes vlc ON vlc.vendor_id = avl.vendor_id " +
            "AND vlc.language_id = :languageId "+
            "WHERE avl.status =:status AND avl.agent_id =:agentId " +
            "AND avl.currency_id = :currencyId " +
            "GROUP BY avl.vendor_id ", nativeQuery=true)
    List<IGameVendor> findByAgentSupportedVendorAndStatus (
            @Param("agentId") int agentId,
            @Param("currencyId") Integer currencyId,
            @Param("languageId") Integer languageId,
            @Param("status") int status);


}

