package com.nextgen.gameaggregator.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.nextgen.gameaggregator.entity.ga.ProductGame;
import com.nextgen.gameaggregator.entity.ga.ProductGameDeactivated;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.DisabledGameException;
import com.nextgen.gameaggregator.exception.GameNotSupportedException;
import com.nextgen.gameaggregator.repository.ga.reader.ProductGameDeactivatedRepository;
import com.nextgen.gameaggregator.repository.ga.reader.ProductGameRepository;


@Service
public class ProductGameServiceImpl implements ProductGameService {

    private final ProductGameRepository productGameRepository;
    private final ProductGameDeactivatedRepository productGameDeactivatedRepository;

    public ProductGameServiceImpl(ProductGameRepository productGameRepository,
                                  ProductGameDeactivatedRepository productGameDeactivatedRepository) {

        this.productGameRepository = productGameRepository;
        this.productGameDeactivatedRepository = productGameDeactivatedRepository;
    }

    @Override
    @Cacheable(value = "ProductGameCodes", key = "#code", cacheManager = "cacheManager", unless = "#result == null")
    public ProductGame getByCode(String code) throws GameNotSupportedException, DisabledGameException {
        ProductGame productGame = productGameRepository.findByCode(code);

        if (productGame == null) {
            throw new GameNotSupportedException();
        }

        if (!productGame.getStatus().equals(Status.ACTIVE.code)) {
            throw new DisabledGameException();
        }

        return productGame;
    }

    @Override
    public void verifyProductGameDeactivated(Integer productGameId, Integer agentId, Integer masterAgentId, Integer houseId) throws DisabledGameException {
        Boolean isDeactivated = this.isProductGameDeactivated(productGameId, agentId, masterAgentId, houseId);
        
        if (Boolean.TRUE.equals(isDeactivated)) {
            throw new DisabledGameException();
        }
    }

    @Cacheable(value = "ProductGameDeactivated", key = "{#productGameId, #agentId, #masterAgentId, #houseId}", cacheManager = "cacheManager", unless = "#result == null")
    public Boolean isProductGameDeactivated(Integer productGameId, Integer agentId, Integer masterAgentId, Integer houseId) {
        ProductGameDeactivated productGameDeactivated = productGameDeactivatedRepository.findProductGameDeactivated(productGameId, agentId, masterAgentId, houseId);

        return productGameDeactivated != null;
    }
}
