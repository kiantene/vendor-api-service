package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.exception.ProductCombinationNotSupportedException;
import com.nextgen.gameaggregator.exception.ProductVendorLineNotFoundException;

public interface AgentProductService {

    Integer getProductVendorLineIdByAgent(Integer productId, Integer gameCategoryId, Integer currencyId, Integer agentId) throws ProductCombinationNotSupportedException, ProductVendorLineNotFoundException;
}
