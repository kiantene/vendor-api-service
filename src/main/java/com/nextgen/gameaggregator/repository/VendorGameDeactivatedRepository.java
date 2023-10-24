package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.VendorGameDeactivated;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorGameDeactivatedRepository extends JpaRepository<VendorGameDeactivated, Integer> {
//    @Cacheable(value = "VendorGameDeactivated", key = "{#vendorGameId, #currencyId}", cacheManager = "cacheManager")
    VendorGameDeactivated findByVendorGameIdAndAgentIdAndIsDeleted(Integer vendorGameId, Integer agentId, Integer isDeleted);

}