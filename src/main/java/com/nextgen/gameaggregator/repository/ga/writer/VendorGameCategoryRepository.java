package com.nextgen.gameaggregator.repository.ga.writer;

import com.nextgen.gameaggregator.entity.ga.VendorGameCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface VendorGameCategoryRepository extends JpaRepository<VendorGameCategory, Integer> {
    VendorGameCategory findByVendorIdAndGameCategoryId(Integer vendorId, Integer gameCategoryId);
}

