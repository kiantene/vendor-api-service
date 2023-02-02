package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.VendorGameCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorGameCodeRepository extends JpaRepository<VendorGameCode, Integer> {
    VendorGameCode findByVendorGameIdAndPlatformIdAndLanguageId(Integer vendorGameId, Integer platformId, Integer languageId);
}
