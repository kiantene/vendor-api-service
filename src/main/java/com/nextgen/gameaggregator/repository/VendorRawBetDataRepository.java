package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.VendorRawBetData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorRawBetDataRepository extends JpaRepository<VendorRawBetData, Long> {

}
