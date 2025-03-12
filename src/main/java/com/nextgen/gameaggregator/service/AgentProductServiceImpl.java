package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.AgentProductDetail;
import com.nextgen.gameaggregator.entity.ga.ProductVendorLine;
import com.nextgen.gameaggregator.exception.ProductCombinationNotSupportedException;
import com.nextgen.gameaggregator.exception.ProductVendorLineNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AgentProductServiceImpl implements AgentProductService {
    private final AgentProductDetailService agentProductDetailService;
    private final ProductVendorLineService productVendorLineService;

    public AgentProductServiceImpl(AgentProductDetailServiceImpl agentProductDetailService,
                                   ProductVendorLineServiceImpl productVendorLineService) {

        this.agentProductDetailService = agentProductDetailService;
        this.productVendorLineService = productVendorLineService;
    }

    @Override
    public Integer getProductVendorLineIdByAgent(Integer productId, Integer gameCategoryId, Integer currencyId, Integer agentId)
            throws ProductCombinationNotSupportedException, ProductVendorLineNotFoundException {

        AgentProductDetail agentProductDetail = agentProductDetailService.getProductDetailByGameCategoryAndCurrency(productId, gameCategoryId, currencyId, agentId);

        if (agentProductDetail == null) throw new ProductCombinationNotSupportedException();

        Integer vendorLineId = agentProductDetail.getVendorLineId();
        boolean shouldFindPriorityVendorLine = vendorLineId == null;

        if (shouldFindPriorityVendorLine) {
            ProductVendorLine productVendorLine = productVendorLineService.getHighestPriorityLine(productId, gameCategoryId, currencyId);
            if (productVendorLine == null) throw new ProductVendorLineNotFoundException();
            vendorLineId = productVendorLine.getVendorLineId();
        }

        return vendorLineId;
    }
}
