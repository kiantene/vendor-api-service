package com.nextgen.gameaggregator.vendor.cq9.api.result;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.eventing.core.EventDispatcherSystem;
import com.nextgen.gameaggregator.eventing.events.BetResultEvent;
import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.exception.DuplicateExternalTransactionIdException;
import com.nextgen.gameaggregator.exception.InvalidAgentApiCredentialException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.vendor.cq9.api.endround.WinDataDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ResultService {
    @Autowired
    private WalletService walletService;

    public BetResultEvent process(String traceId, GameSession gameSession, WinDataDto winDataDto, String body) throws InvalidRequestException, BetNotFoundException, DuplicateExternalTransactionIdException, InvalidAgentApiCredentialException {

        // Construct result dto
        BetResultEvent betResultEvent = walletService.processWin(traceId, gameSession, winDataDto, body);
        EventDispatcherSystem.emitAsync(betResultEvent);

        return betResultEvent;
    }
}
