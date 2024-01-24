package com.nextgen.gameaggregator.repository.ga.writer;

import com.nextgen.gameaggregator.entity.ga.VendorLanguageCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VendorLanguageCodeRepository extends JpaRepository<VendorLanguageCode, Integer> {
    VendorLanguageCode findByVendorIdAndLanguageId(Integer vendorId, Integer languageId);

    List<VendorLanguageCode> findByVendorId(Integer vendorId);

    VendorLanguageCode findTop1ByVendorIdAndStatus(Integer vendorId, Integer status);
}
