package com.nextgen.gameaggregator.vendor.lucky365.api.result;

import com.nextgen.core.exception.InternalServerException;
import com.nextgen.gameaggregator.vendor.lucky365.api.bet.BetRequest;
import com.nextgen.gameaggregator.vendor.lucky365.api.bet.BetResponse;
import org.springframework.stereotype.Service;

@Service
public class MultiBetResultService {

    public BetResultResponse process(BetResultRequest betResultRequest) {
        // we cannot handle multiple bet result list
        throw new InternalServerException("Multiple bet results are not allowed");
    }
}
