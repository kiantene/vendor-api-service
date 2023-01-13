package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.VendorGame;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorGameRepository extends JpaRepository<VendorGame, Integer> {
    VendorGame findByCode(String code);
    VendorGame findByIdAndStatus(Integer id, Integer status);
    VendorGame findByVendorGameCode(String vendorGameCode);
    VendorGame findByVendorGameCodeAndVendorId(String vendorGameCode, Integer vendorId);
    @Query(value =" SELECT vg.code as gameCode, vg.name as gameName, gc.code as categoryCode FROM vendor_games as vg " +
            "INNER JOIN game_categories as gc ON gc.id = vg.game_category_id WHERE vg.vendor_id=:vendorId AND vg.status=:status",
            countQuery =
                    "SELECT count(*) FROM vendor_games WHERE vendor_id=:vendorId AND status=:status",
            nativeQuery=true)
    Page<Object> findByVendorIdAndStatus(@Param("vendorId") Integer vendorId, @Param("status") Integer status, Pageable pageable);
}
