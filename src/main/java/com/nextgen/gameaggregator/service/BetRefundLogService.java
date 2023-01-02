package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.BetRefundLog;
import com.nextgen.gameaggregator.entity.BetResultLog;
import com.nextgen.gameaggregator.repository.BetRefundLogRepository;
import com.nextgen.gameaggregator.repository.BetResultLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BetRefundLogService {
    @Autowired
    private BetRefundLogRepository betRefundLogRepository;

    /**
     * Creates a database record of the given BetRefundLog entity object.
     * This function will also populate default values of certain fields.
     * Every time a Bet Refund is received, a new record will be created.
     *
     * @param entity BetRefundLog entity object containing vendor's unique transaction Id to reverse the bet
     * @return BetRefundLog entity object after a successful save
     */
    public BetRefundLog create(BetRefundLog entity) {
        // Set default values
        entity.setStatus(1); // TODO: refactor, map to constant/enum value
        entity.setCreateTime(System.currentTimeMillis());

        return betRefundLogRepository.save(entity);
    }
}
