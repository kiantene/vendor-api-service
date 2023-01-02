package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.BetResultLog;
import com.nextgen.gameaggregator.repository.BetResultLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BetResultLogService {
    @Autowired
    private BetResultLogRepository betResultLogRepository;

    /**
     * Creates a database record of the given BetResultLog entity object.
     * This function will also populate default values of certain fields.
     * Every time a Bet Result is received, a new record will be created.
     *
     * @param entity BetResultLog entity object containing the result of a previous bet
     * @return BetResultLog entity object after a successful save
     */
    public BetResultLog create(BetResultLog entity) {
        // Set default values
        entity.setStatus(1); // TODO: refactor, map to constant/enum value
        entity.setCreateTime(System.currentTimeMillis());

        return betResultLogRepository.save(entity);
    }
}
