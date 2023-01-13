package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, Integer> {
    Vendor findByCode(String code);
}

