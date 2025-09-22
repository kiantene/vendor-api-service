package com.nextgen.gameaggregator.core.engine.wallet.result;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.entity.couchbase.AgentMeta;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.service.business.GameRoundService;
import com.nextgen.gameaggregator.service.business.GameTransactionService;
import com.nextgen.gameaggregator.service.data.producer.BetHistoryProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
class BetResultProcessor {
    private final BetResultDataMapper betResultDataMapper;
    private final BetHistoryProducer betHistoryProducer;
    private final GameRoundService gameRoundService;
    private final GameTransactionService gameTransactionService;
    private final WalletService walletService;

    public PlayerBalanceData processResultTransaction(
            BetResultContext context,
            GameSession gameSession,
            GameTransaction txn,
            ResultType resultType,
            HttpRequestLog httpRequestLog,
            BetResultWrapperContext state) throws
            InvalidAgentApiCredentialException, VendorCurrencyNotSupportException,
            BetResultIdempotentViolationException, MergedBetDataIntegrityException,
            InsufficientBalanceException, TransactionStillProcessingException,
            BetNotFoundException, InvalidOperatorResponseException, InternalServerTimeoutRetryException {

        BetResultConfig config = state.getConfig();
        GameRound round = gameTransactionService.markSent(txn, buildAgentMeta(context, gameSession));

        /**
         * If we receive result txn first before the bet txn arrives, we do not send the result to operator.
         * Instead, we wait for bet and send it together
         */
        boolean isResultBeforeBetEnabled = config.isAllowResultBeforeBet();
        if (isResultBeforeBetEnabled && gameRoundService.isResultBeforeBet(round, resultType)) {
            gameTransactionService.markPending(txn);
            return PlayerBalanceData.getDefault(context.getVendorPlayerUsername(), context.getVendorCurrency());
        }

        if (config.isSettledByRound()) {
            // disable produce bet history in WalletService
            httpRequestLog.setBetHistoryProduceDisabled(true);
        }

        BigDecimal balance = walletService.processBetResult(
                httpRequestLog.getId(),
                gameSession,
                betResultDataMapper.toBetResultData(context),
                resultType,
                state.getVendorService(),
                httpRequestLog
        );
        txn.setGaBetId(httpRequestLog.getGaBetId());
        GameRound updatedRound = gameTransactionService.markSuccess(round, txn, balance, context.isRoundEnded());

        if (config.isSettledByRound() && context.isRoundEnded()) {
            betHistoryProducer.publishBetHistoryByRound(context, updatedRound, txn);
        }

        return new PlayerBalanceData(
                context.getVendorPlayerUsername(),
                context.getVendorCurrency(),
                balance,
                httpRequestLog.getOperatorEnd()
        );
    }

    public PlayerBalanceData processBetAndResultTransaction(
            BetResultContext context,
            GameSession gameSession,
            GameTransaction txn,
            ResultType resultType,
            HttpRequestLog httpRequestLog,
            BetResultWrapperContext state) throws
            InvalidAgentApiCredentialException, VendorCurrencyNotSupportException,
            BetResultIdempotentViolationException, MergedBetDataIntegrityException,
            InsufficientBalanceException, TransactionStillProcessingException,
            BetNotFoundException, InvalidOperatorResponseException, InternalServerTimeoutRetryException {

        GameRound round = gameTransactionService.markSent(txn, buildAgentMeta(context, gameSession));
        BigDecimal balance = walletService.processBetResult(
                httpRequestLog.getId(),
                gameSession,
                betResultDataMapper.toBetResultData(context),
                resultType,
                state.getVendorService(),
                httpRequestLog
        );
        txn.setGaBetId(httpRequestLog.getGaBetId());
        gameTransactionService.markSuccess(round, txn, balance, context.isRoundEnded());

        return new PlayerBalanceData(
                context.getVendorPlayerUsername(),
                context.getVendorCurrency(),
                balance,
                httpRequestLog.getOperatorEnd()
        );
    }

    private AgentMeta buildAgentMeta(BetResultContext context, GameSession gameSession) {
        AgentMeta agentMeta = new AgentMeta();
        agentMeta.setAgentId(context.getAgentId());
        agentMeta.setUsername(context.getAgentPlayerUsername());
        agentMeta.setCurrency(context.getCurrencyCode());
        agentMeta.setGameCode(context.getGameCode());
        agentMeta.setSession(gameSession.getToken());

        return agentMeta;
    }
}
