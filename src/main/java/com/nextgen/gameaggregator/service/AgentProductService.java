package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.AgentProductDetail;
import com.nextgen.gameaggregator.exception.ProductAccessDeniedException;
import com.nextgen.gameaggregator.exception.ProductCombinationNotSupportedException;

public interface AgentProductService {

    AgentProductDetail getProductAgentVendorLine(Integer productId, Integer gameCategoryId, Integer currencyId, Integer agentId) throws ProductAccessDeniedException, ProductCombinationNotSupportedException;
}
