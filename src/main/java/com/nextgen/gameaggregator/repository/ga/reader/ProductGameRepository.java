package com.nextgen.gameaggregator.repository.ga.reader;

import com.nextgen.gameaggregator.entity.ga.ProductGame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductGameRepository extends JpaRepository<ProductGame, Integer> {
    @Query("SELECT p FROM ProductGame p WHERE p.code = :code OR p.vendorGameCode = :code")
    ProductGame findByCode(String code);
} 
