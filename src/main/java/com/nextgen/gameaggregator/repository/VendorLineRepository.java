package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.VendorLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorLineRepository extends JpaRepository<VendorLine, Integer> {

}
