package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.AgentProductDetail;
import com.nextgen.gameaggregator.repository.ga.reader.AgentProductDetailRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgentProductDetailServiceImpl implements AgentProductDetailService {

    private final AgentProductDetailRepository agentProductDetailRepository;

    public AgentProductDetailServiceImpl(AgentProductDetailRepository agentProductDetailRepository) {
        this.agentProductDetailRepository = agentProductDetailRepository;
    }

    @Cacheable(value = "ProductDetailByGameCategoryAndCurrency", key = "{#productId, #gameCategoryId, #currencyId, #agentId}", cacheManager = "cacheManager")
    @Override
    public AgentProductDetail getProductDetailByGameCategoryAndCurrency(Integer productId, Integer gameCategoryId, Integer currencyId, Integer agentId) {

        List<AgentProductDetail> agentProductDetailList = agentProductDetailRepository.findVendorLine(productId, agentId);

        if (agentProductDetailList.isEmpty()) return null;

        List<AgentProductDetail> listOfSupportedCategory = agentProductDetailList.stream()
                .filter(productDetail -> productDetail.getGameCategoryId().equals(gameCategoryId))
                .toList();

        // means the product combination (eg. game category/currency) not supported for this agent
        if (listOfSupportedCategory.isEmpty()) return null;

        return listOfSupportedCategory.stream()
                .filter(productDetail -> productDetail.getCurrencyId().equals(currencyId))
                .findFirst()
                .orElse(null);
    }
}
