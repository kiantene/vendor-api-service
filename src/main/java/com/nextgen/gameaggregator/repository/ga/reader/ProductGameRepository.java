package com.nextgen.gameaggregator.repository.ga.reader;

import com.nextgen.gameaggregator.entity.ga.ProductGame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductGameRepository extends JpaRepository<ProductGame, Integer> {
    ProductGame findByCode(String code);
} 
