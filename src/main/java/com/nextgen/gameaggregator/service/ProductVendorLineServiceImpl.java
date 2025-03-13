package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.ProductVendorLine;
import com.nextgen.gameaggregator.repository.ga.reader.ProductVendorLineRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class ProductVendorLineServiceImpl implements ProductVendorLineService {

    private final ProductVendorLineRepository productVendorLineRepository;

    public ProductVendorLineServiceImpl(ProductVendorLineRepository productVendorLineRepository) {
        this.productVendorLineRepository = productVendorLineRepository;
    }

    @Override
    @Cacheable(value = "HighestPriorityLine", key = "{#productId, #gameCategoryId, #currencyId}", cacheManager = "cacheManager", unless = "#result == null")
    public ProductVendorLine getHighestPriorityLine(Integer productId, Integer gameCategoryId, Integer currencyId) {
        List<ProductVendorLine> listOfLines = productVendorLineRepository.findByProductIdAndGameCategoryIdAndCurrencyId(productId, gameCategoryId, currencyId);

        if (listOfLines.isEmpty()) return null;

        // Filter out lines where priority is greater than 0 and sort by priority
        Optional<ProductVendorLine> highestPriorityLine = listOfLines.stream()
                .filter(line -> line.getPriority() > 0)
                .sorted(Comparator.comparingInt(ProductVendorLine::getPriority))
                .findFirst();

        // Return the highest priority line, or the first one if all priorities are 0
        return highestPriorityLine.orElse(listOfLines.get(0));
    }
}
