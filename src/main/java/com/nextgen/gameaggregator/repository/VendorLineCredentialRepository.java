package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.VendorLineCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VendorLineCredentialRepository extends JpaRepository<VendorLineCredential, Integer> {
    VendorLineCredential findByVendorLineIdAndNameAndStatus(Integer vendorLineId, String name, Integer status);

    VendorLineCredential findByNameAndValueAndStatus(String name, String value, Integer status);

    @Query(value = "SELECT vlc.vendor_line_id FROM vendor_line_credentials vlc WHERE vlc.name = :name AND vlc.value = :value AND vlc.status = :status", nativeQuery = true)
    List<Integer> findVendorLineIdByNameAndValueAndStatus(@Param("name") String name, @Param("value") String value, @Param("status") Integer status);
}
