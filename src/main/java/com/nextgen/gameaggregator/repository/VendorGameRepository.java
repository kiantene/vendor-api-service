package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.VendorGame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorGameRepository extends JpaRepository<VendorGame, Integer> {

}
