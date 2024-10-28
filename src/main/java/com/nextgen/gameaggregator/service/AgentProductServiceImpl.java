package com.nextgen.gameaggregator.service;

import java.util.List;
import java.util.Optional;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.nextgen.gameaggregator.entity.ga.AgentProductDetail;
import com.nextgen.gameaggregator.exception.ProductAccessDeniedException;
import com.nextgen.gameaggregator.exception.ProductCombinationNotSupportedException;
import com.nextgen.gameaggregator.repository.ga.reader.AgentProductDetailRepository;

@Service
public class AgentProductServiceImpl implements AgentProductService {
    private final AgentProductDetailRepository agentProductDetailRepository;

    public AgentProductServiceImpl(AgentProductDetailRepository agentProductDetailRepository) {
        this.agentProductDetailRepository = agentProductDetailRepository;
    }

    @Cacheable(value = "ProductAgentVendorLine", key = "{#productId, #gameCategoryId, #currencyId, #agentId}", cacheManager = "cacheManager", unless = "#result == null")
    public AgentProductDetail getProductAgentVendorLine(Integer productId, Integer gameCategoryId, Integer currencyId, Integer agentId) throws 
        ProductAccessDeniedException, ProductCombinationNotSupportedException {
        List<AgentProductDetail> agentProductDetailList = agentProductDetailRepository.findVendorLine(productId, agentId);

        if (agentProductDetailList.isEmpty()) {
            throw new ProductAccessDeniedException();
        }

        List<AgentProductDetail> listOfSupportedCategory = agentProductDetailList.stream()
                .filter(productDetail -> productDetail.getGameCategoryId().equals(gameCategoryId))
                .toList();

        // means the product combination (eg. game category/currency) not supported for this agent
        if (listOfSupportedCategory.isEmpty()) {
            throw new ProductCombinationNotSupportedException();
        }

        Optional<AgentProductDetail> supportedCurrency = listOfSupportedCategory.stream()
                .filter(productDetail -> productDetail.getCurrencyId().equals(currencyId))
                .findFirst();

        return supportedCurrency.orElseThrow(ProductCombinationNotSupportedException::new);
    }
}
