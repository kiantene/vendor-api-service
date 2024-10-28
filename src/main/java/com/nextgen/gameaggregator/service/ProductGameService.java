package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.ProductGame;
import com.nextgen.gameaggregator.exception.DisabledGameException;
import com.nextgen.gameaggregator.exception.GameNotSupportedException;

public interface ProductGameService {

    ProductGame getByCode(String code) throws GameNotSupportedException, DisabledGameException;
    void verifyProductGameDeactivated(Integer productGameId, Integer agentId, Integer masterAgentId, Integer houseId) throws DisabledGameException;
}
