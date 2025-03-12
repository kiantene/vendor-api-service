package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.ProductGame;
import com.nextgen.gameaggregator.exception.DisabledGameException;
import com.nextgen.gameaggregator.exception.GameNotSupportedException;

public interface ProductGameService {

    ProductGame getByCode(String code);
    boolean isGameDeactivated(Integer productGameId, Integer agentId, Integer masterAgentId, Integer houseId);
    void checkGameStatus(ProductGame productGame) throws GameNotSupportedException, DisabledGameException;
}
