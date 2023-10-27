package com.nextgen.gameaggregator.sport.service;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class WalletService {

    public BetEvent placeBet(String traceId, GameSession gameSession, BetResultData betResultData, HttpRequestLog httpRequestLog) {
        

        return null;
    }

    public BetEvent confirmBet() {

        return null;
    }

    public BetEvent settle() {

        return null;
    }
}