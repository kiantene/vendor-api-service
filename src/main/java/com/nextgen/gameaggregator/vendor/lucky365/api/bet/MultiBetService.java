package com.nextgen.gameaggregator.vendor.lucky365.api.bet;

import com.nextgen.core.exception.InternalServerException;
import org.springframework.stereotype.Service;


@Service
public class MultiBetService {

    public BetResponse process(BetRequest betRequest) {
        // we cannot handle multiple bet list
        throw new InternalServerException("Multiple bets are not allowed");
    }
}
