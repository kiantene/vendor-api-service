package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.VendorLineCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorLineCredentialRepository extends JpaRepository<VendorLineCredential, Integer> {
    VendorLineCredential findByVendorLineIdAndNameAndStatus(Integer vendorLineId, String name, Integer status);

    VendorLineCredential findByNameAndValueAndStatus(String name, String value, Integer status);
}
