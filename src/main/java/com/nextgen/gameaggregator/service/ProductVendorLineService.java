package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.ProductVendorLine;
import com.nextgen.gameaggregator.exception.ProductVendorLineNotFoundException;

public interface ProductVendorLineService {
    ProductVendorLine getHighestPriorityLine(Integer productId, Integer gameCategoryId, Integer currencyId) throws ProductVendorLineNotFoundException;
}
