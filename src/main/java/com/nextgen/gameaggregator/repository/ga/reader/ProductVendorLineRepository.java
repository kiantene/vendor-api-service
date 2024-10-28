package com.nextgen.gameaggregator.repository.ga.reader;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nextgen.gameaggregator.entity.ga.ProductVendorLine;

@Repository
public interface ProductVendorLineRepository extends JpaRepository<ProductVendorLine, Integer> {
    List<ProductVendorLine> findByProductIdAndGameCategoryIdAndCurrencyId(Integer productId, Integer gameCategoryId, Integer currencyId);
}
