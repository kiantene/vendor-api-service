package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.ProductGame;
import com.nextgen.gameaggregator.entity.ga.ProductGameDeactivated;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.DisabledGameException;
import com.nextgen.gameaggregator.exception.GameNotSupportedException;
import com.nextgen.gameaggregator.repository.ga.reader.ProductGameDeactivatedRepository;
import com.nextgen.gameaggregator.repository.ga.reader.ProductGameRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

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
    @Cacheable(value = "ProductGameCodes", key = "#code", cacheManager = "cacheManager")
    public ProductGame getByCode(String code) {
        return productGameRepository.findByCode(code);
    }

    @Cacheable(value = "ProductGameDeactivated", key = "{#productGameId, #agentId, #masterAgentId, #houseId}", cacheManager = "cacheManager")
    public boolean isGameDeactivated(Integer productGameId, Integer agentId, Integer masterAgentId, Integer houseId) {
        ProductGameDeactivated productGameDeactivated = productGameDeactivatedRepository.findProductGameDeactivated(productGameId, agentId, masterAgentId, houseId);

        return productGameDeactivated != null;
    }

    public void checkGameStatus(ProductGame productGame) throws GameNotSupportedException, DisabledGameException {
        if (productGame == null) throw new GameNotSupportedException();
        if (productGame.getStatus().equals(Status.INACTIVE.code)) {
            throw new DisabledGameException();
        }
    }
}
