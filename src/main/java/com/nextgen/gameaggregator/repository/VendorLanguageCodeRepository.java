package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.VendorLanguageCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorLanguageCodeRepository extends JpaRepository<VendorLanguageCode, Integer> {
    VendorLanguageCode findByVendorIdAndLanguageId(Integer vendorId, Integer languageId);
}
