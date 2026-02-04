package com.nextgen.gameaggregator.core.service;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.engine.wallet.BetResultDataMapper;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultWrapperContext;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WalletLegacyService {

    private final WalletService walletService;
    private final BetResultDataMapper betResultDataMapper;

    public PlayerBalanceData processBet(
            HttpRequestLog httpRequestLog,
            GameSession gameSession,
            BetContext context,
            GameTransaction txn) throws
            InvalidAgentApiCredentialException, VendorCurrencyNotSupportException,
            BetResultIdempotentViolationException, InsufficientBalanceException,
            TransactionStillProcessingException, InvalidOperatorResponseException,
            CouchbaseDataIntegrityException{

        BetEvent betEvent = walletService.processBet(
                httpRequestLog.getId(),
                gameSession,
                betResultDataMapper.toBetResultData(context, txn.getGaBetId()),
                httpRequestLog.getRequestBody(),
                httpRequestLog
        );
//        onAfterSendBet(round, txn, betEvent.getLastBalance());

        return new PlayerBalanceData(
                context.getVendorPlayerUsername(),
                context.getVendorCurrency(),
                betEvent.getLastBalance(),
                httpRequestLog.getOperatorEnd()
        );
    }

    public PlayerBalanceData processResult(
            HttpRequestLog httpRequestLog,
            GameSession gameSession,
            BetResultWrapperContext betResultWrapperContext,
            ResultType resultType,
            GameTransaction resultTxn) throws
            InvalidAgentApiCredentialException, VendorCurrencyNotSupportException,
            BetResultIdempotentViolationException, MergedBetDataIntegrityException,
            InsufficientBalanceException, TransactionStillProcessingException,
            InvalidOperatorResponseException, InternalServerTimeoutRetryException,
            com.nextgen.gameaggregator.exception.BetNotFoundException {

        BetResultContext context = betResultWrapperContext.getBetResultContext();

        BigDecimal balance = walletService.processBetResult(
                httpRequestLog.getId(),
                gameSession,
                betResultDataMapper.toBetResultData(context),
                resultType,
                betResultWrapperContext.getVendorService(),
                httpRequestLog
        );
        resultTxn.setGaBetId(httpRequestLog.getGaBetId());

        return new PlayerBalanceData(
                context.getVendorPlayerUsername(),
                context.getVendorCurrency(),
                balance,
                httpRequestLog.getOperatorEnd()
        );
    }
}
