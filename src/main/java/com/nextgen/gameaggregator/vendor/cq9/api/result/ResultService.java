package com.nextgen.gameaggregator.vendor.cq9.api.result;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.eventing.events.BetResultEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.vendor.cq9.api.endround.WinDataDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ResultService {
    @Autowired
    private WalletService walletService;

    public BetResultEvent process(String traceId, GameSession gameSession, WinDataDto winDataDto, String body) throws BetNotFoundException, DuplicateExternalTransactionIdException, InvalidAgentApiCredentialException, InvalidOperatorResponseException, BetResultNotFoundException {

        // Construct result dto
        return walletService.processWin(traceId, gameSession, winDataDto, body);
    }
}
