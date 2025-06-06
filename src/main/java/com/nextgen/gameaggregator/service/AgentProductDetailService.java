package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.AgentProductDetail;

public interface AgentProductDetailService {
    AgentProductDetail getProductDetailByGameCategoryAndCurrency(Integer productId, Integer gameCategoryId, Integer currencyId, Integer agentId);
}
