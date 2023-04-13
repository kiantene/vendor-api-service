package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.VendorGameCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VendorGameCodeRepository extends JpaRepository<VendorGameCode, Integer> {
    VendorGameCode findByVendorGameIdAndPlatformIdAndLanguageId(Integer vendorGameId, Integer platformId, Integer languageId);

    VendorGameCode findByVendorGameIdAndPlatformIdAndLanguageIdAndStatus(Integer vendorGameId, Integer platformId, Integer languageId, Integer status);

    List<VendorGameCode> findByVendorGameId(Integer vendorGameId);

    List<VendorGameCode> findByVendorGameIdAndLanguageId(Integer vendorGameId,  Integer languageId);

    List<VendorGameCode> findByVendorGameIdAndLanguageIdAndStatus(Integer vendorGameId,  Integer languageId, Integer status);



}
