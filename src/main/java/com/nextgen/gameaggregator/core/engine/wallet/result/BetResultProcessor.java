package com.nextgen.gameaggregator.core.engine.wallet.result;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.exception.BetNotFoundException;
import com.nextgen.gameaggregator.entity.couchbase.AgentMeta;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.entity.couchbase.RoundTxn;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.service.business.GameTransactionService;
import com.nextgen.gameaggregator.service.data.producer.BetHistoryProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
class BetResultProcessor {
    private final BetResultDataMapper betResultDataMapper;
    private final BetHistoryProducer betHistoryProducer;
    private final GameTransactionService gameTransactionService;
    private final WalletService walletService;
    private final BetResultLifeCycleRegistry lifeCycleRegistry;

    public PlayerBalanceData processResultTransaction(
            BetResultContext context,
            GameSession gameSession,
            GameTransaction resultTxn,
            ResultType resultType,
            HttpRequestLog httpRequestLog,
            BetResultWrapperContext state) throws
            InvalidAgentApiCredentialException, VendorCurrencyNotSupportException,
            BetResultIdempotentViolationException, MergedBetDataIntegrityException,
            InsufficientBalanceException, TransactionStillProcessingException,
            InvalidOperatorResponseException, InternalServerTimeoutRetryException,
            com.nextgen.gameaggregator.exception.BetNotFoundException {

        BetResultConfig config = state.getConfig();
        GameRound round = gameTransactionService.markSent(resultTxn, AgentMeta.of(context, gameSession.getToken()));

        /**
         * If we receive result txn first before the bet txn arrives, we do not send the result to operator.
         * Instead, we wait for bet and send it together
         */
        BetResultDecision decision = BetResultPolicy.decideResultBeforeBet(round, config);
        decision.throwIfRejected(context, config);

        if (decision.isAllowed()) {
            // TODO: this logic is not implemented yet
            gameTransactionService.markPending(resultTxn);
            return PlayerBalanceData.getDefault(context.getVendorPlayerUsername(), context.getVendorCurrency());
        }

        // To settle the unsettled bet
        doSettlement(config, context, round, resultTxn);

        /**
         * Use the Strategy Pattern to execute vendor specific logic before the wallet call.
         * The handler is retrieved from the registry based on the transaction's vendor class name.
         */
        String vendorClassName = context.getVendorClassName();
        BetResultLifeCycle handler = lifeCycleRegistry.getHandler(vendorClassName);

        if (handler != null) {
            handler.onBeforeSend(gameSession, context);
        }

        if (config.isSettledByRound() || !config.isPublishBetHistoryOnRoundEnded()) {
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
        resultTxn.setGaBetId(httpRequestLog.getGaBetId());
        GameRound updatedRound = gameTransactionService.markSuccess(round, resultTxn, balance, context.isRoundEnded());

        if (config.isSettledByRound() && context.isRoundEnded()) {
            betHistoryProducer.publishBetHistoryByRound(context, updatedRound, resultTxn);
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
            InvalidOperatorResponseException, InternalServerTimeoutRetryException,
            com.nextgen.gameaggregator.exception.BetNotFoundException {

        GameRound round = gameTransactionService.markSent(txn, AgentMeta.of(context, gameSession.getToken()));

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

    private void doSettlement(BetResultConfig config, BetResultContext context, GameRound round, GameTransaction resultTxn) {
        if (config.isSettledByBet()) {
            settleSpecificBet(round, resultTxn);
        } else if (context.isRoundEnded()) {
            settleAllUnsettledBets(round, resultTxn);
        }
    }

    private void settleSpecificBet(GameRound round, GameTransaction resultTxn) {
        var betTxn = findUnsettledBet(round, resultTxn.getVendorBetId())
                .orElseThrow(() -> new BetNotFoundException(
                        round.getId() + " : " + resultTxn.getVendorBetId() + " cannot find unsettled bet"));

        gameTransactionService.markSettled(betTxn.getId(), resultTxn.getSettleTime());

        if (betTxn.hasAliasTxn(round.getClassName())) {
            gameTransactionService.markSettled(betTxn.getRollbackId(round.getClassName()), resultTxn.getSettleTime());
        }
    }

    private void settleAllUnsettledBets(GameRound round, GameTransaction resultTxn) {
        getUnsettledBets(round)
                .forEach(t -> gameTransactionService.markSettled(t.getId(), resultTxn.getSettleTime()));
    }

    private Optional<RoundTxn> findUnsettledBet(GameRound round, String vendorBetId) {
        return getUnsettledBets(round)
                .filter(txn -> txn.getVendorBetId().equals(vendorBetId))
                .findFirst();
    }

    private Stream<RoundTxn> getUnsettledBets(GameRound round) {
        return round.getTransactions().stream()
                .filter(RoundTxn::isUnsettled)
                .filter(RoundTxn::isSuccessfulBet);
    }
}
