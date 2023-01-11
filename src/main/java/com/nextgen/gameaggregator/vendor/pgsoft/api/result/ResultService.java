package com.nextgen.gameaggregator.vendor.pgsoft.api.result;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.eventing.events.BetResultEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ResultService {
    @Autowired
    private WalletService walletService;
    public BetResultEvent process(String traceId, GameSession gameSession, String body) throws InvalidRequestException, BetNotFoundException, DuplicateExternalTransactionIdException, InvalidAgentApiCredentialException, InvalidOperatorResponseException {

        // Construct result dto
        ResultDto dto = HttpService.convertQueryStringToDto(body, ResultDto.class);
        return walletService.processWin(traceId, gameSession, dto, body);

    }

}
