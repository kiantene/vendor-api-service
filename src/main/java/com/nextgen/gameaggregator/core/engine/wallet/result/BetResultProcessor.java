package com.nextgen.gameaggregator.core.engine.wallet.result;

import com.nextgen.gameaggregator.core.context.OperatorRequestContext;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.operator.*;
import com.nextgen.gameaggregator.core.engine.operator.wallet.result.*;
import com.nextgen.gameaggregator.core.engine.wallet.BetResultDataMapper;
import com.nextgen.gameaggregator.core.exception.BetNotFoundException;
import com.nextgen.gameaggregator.core.service.WalletLegacyService;
import com.nextgen.gameaggregator.core.vendor.config.VendorConfigService;
import com.nextgen.gameaggregator.entity.couchbase.AgentMeta;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.entity.couchbase.RoundTxn;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.service.business.GameTransactionService;
import com.nextgen.gameaggregator.service.data.producer.BetHistoryProducer;
import com.nextgen.gameaggregator.service.data.producer.transactionhistory.BetTransactionHistoryProducer;
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
    private final VendorConfigService vendorConfigService;
    private final BetTransactionHistoryProducer betTransactionHistoryProducer;
    private final BetResultLifeCycleRegistry lifeCycleRegistry;
    private final WalletLegacyService walletLegacyService;
    private final OperatorApiService operatorApiService;
    private final SettleByBetOperatorBetRequestMapper settleByBetOperatorBetRequestMapper;
    private final SettleByRoundOperatorBetResultRequestMapper settleByRoundOperatorBetResultRequestMapper;
    private final BetAndResultOperatorBetResultRequestMapper betAndResultOperatorBetResultRequestMapper;
    private final BetResultOperatorWalletAdapter betResultOperatorWalletAdapter;

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

        Optional<RoundTxn> betTxn = onBeforeSendResult(context, gameSession, resultTxn, httpRequestLog, config, round);

        GameTransaction betFullTxn = null;
        PlayerBalanceData balanceData = null;
        if (vendorConfigService.isWalletServiceLegacyEnabled(context.getVendorClassName())) {// || config.isSettledByRound()) {

            balanceData = walletLegacyService.processResult(httpRequestLog, gameSession, state, resultType, resultTxn);

            resultTxn.setGaBetId(httpRequestLog.getGaBetId());
        } else {
            /**
             * Bet Txn is Always Present for SettleByBet if not BetNotFoundException would have been thrown earlier
             */
            betFullTxn = gameTransactionService.getOrThrow(betTxn.get().getId());
            if (config.isSettledByRound()) {
                if (context.isRoundEnded()) {
                    SettleByRoundScenario scenario = new SettleByRoundScenario(resultType, betFullTxn);

                    OperatorBetResultRequest operatorRequest =
                            settleByRoundOperatorBetResultRequestMapper.toOperatorRequest(
                                    OperatorApiContext.of(context, round, resultTxn),
                                    scenario
                            );

                    // Will Send 1 Final Result to Operator when Round is Ended
                    balanceData = callToOperator(context, round, resultTxn, operatorRequest, scenario);
                } else {
                    // Return LastKnown Balance from GameRound? Or should we do GetBalance from Operator?
                    balanceData = PlayerBalanceData.getDefaultWithBalance(context.getVendorPlayerUsername(), context.getVendorCurrency(), round.getLastBalance());
                }
            } else {
                SettleByBetScenario scenario = new SettleByBetScenario(resultType, betFullTxn);

                OperatorBetResultRequest operatorRequest = settleByBetOperatorBetRequestMapper.toOperatorRequest(
                        OperatorApiContext.of(context, round, resultTxn),
                        scenario
                );

                balanceData = callToOperator(context, round, resultTxn, operatorRequest, scenario);
            }

            resultTxn.setGaBetId(betFullTxn.getGaBetId());
        }

        onAfterSendResult(context, resultTxn, betFullTxn, round, balanceData, config);

        return balanceData;
    }

    private Optional<RoundTxn> onBeforeSendResult(
            BetResultContext context,
            GameSession gameSession,
            GameTransaction resultTxn,
            HttpRequestLog httpRequestLog,
            BetResultConfig config,
            GameRound round) {

        // To settle the unsettled bet
        Optional<RoundTxn> betTxn = doSettlement(config, context, round, resultTxn);

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

        // Send Transaction Historu to Kafka
        if (vendorConfigService.isTransactionHistoryEnabled(context.getVendorClassName())) {
            betTransactionHistoryProducer.publishTransactionHistoryForResult(context, round, resultTxn);
        }

        return betTxn;
    }

    private void onAfterSendResult(BetResultContext context, GameTransaction resultTxn, GameTransaction betFullTxn, GameRound round, PlayerBalanceData balanceData, BetResultConfig config) {
        GameRound updatedRound = gameTransactionService.markSuccess(round, resultTxn, balanceData.getBalance(), context.isRoundEnded());

        if (config.isSettledByRound() && context.isRoundEnded()) {
            /**
             * TODO: Remove the Null Check when Wallet Legacy is Removed
             * betFullTxn == null for Wallet Legacy
             */
            if (betFullTxn == null) {
                RoundTxn betTxn = findFirstValidBet(round);
                betFullTxn = gameTransactionService.getOrThrow(betTxn.getId());
            }

            betHistoryProducer.publishBetHistoryByRound(context, updatedRound, resultTxn, betFullTxn);

        } else if (config.isSettledByBet() && betFullTxn != null) {
            /**
             * TODO: Remove the Null Check when Wallet Legacy is Removed
             * betFullTxn == null for Wallet Legacy
             */
            betHistoryProducer.publishBetHistoryForResult(context, round, betFullTxn, resultTxn);
        }
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

        GameRound round = onBeforeSendBetAndResult(context, gameSession, txn);

        // TODO: Verify when we migrate an existing BetAndResult Vendor
        if (vendorConfigService.isWalletServiceLegacyEnabled(context.getVendorClassName())) {
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
        } else {
            BetAndResultScenario scenario = new BetAndResultScenario(resultType);

            OperatorBetResultRequest operatorRequest = betAndResultOperatorBetResultRequestMapper.toOperatorRequest(
                    OperatorApiContext.of(context, round, txn),
                    new BetAndResultScenario(resultType)
            );

            PlayerBalanceData balanceData = callToOperator(context, round, txn, operatorRequest, scenario);

            onAfterSendBetAndResult(context, txn, round, balanceData);

            return balanceData;
        }
    }

    private GameRound onBeforeSendBetAndResult(BetResultContext context, GameSession gameSession, GameTransaction txn) {
        GameRound round = gameTransactionService.markSent(txn, AgentMeta.of(context, gameSession.getToken()));
        return round;
    }

    private void onAfterSendBetAndResult(BetResultContext context, GameTransaction txn, GameRound round, PlayerBalanceData balanceData) {
        gameTransactionService.markSuccess(round, txn, balanceData.getBalance(), context.isRoundEnded());

        // Legacy Wallet Service would have already send the Bet History
        if (!vendorConfigService.isWalletServiceLegacyEnabled(context.getVendorClassName())) {
            betHistoryProducer.publishBetHistoryForBetAndResult(context, round, txn);
        }

        // Send Kafka
        if (vendorConfigService.isTransactionHistoryEnabled(context.getVendorClassName())) {
            betTransactionHistoryProducer.publishTransactionHistoryForBetAndResult(context, round, txn);
        }
    }

    private Optional<RoundTxn> doSettlement(BetResultConfig config, BetResultContext context, GameRound round, GameTransaction resultTxn) {
        if (config.isSettledByBet()) {
            return Optional.of(settleSpecificBet(round, resultTxn));
        } else if (context.isRoundEnded()) {
            settleAllUnsettledBets(round, resultTxn);
        }

        if (config.isSettledByRound()) {
            return Optional.of(findFirstValidBet(round));
        } else {
            return Optional.empty();
        }
    }

    private RoundTxn settleSpecificBet(GameRound round, GameTransaction resultTxn) {
        var betTxn = findUnsettledBet(round, resultTxn.getVendorBetId())
                .orElseThrow(() -> new BetNotFoundException(
                        round.getId() + " : " + resultTxn.getVendorBetId() + " cannot find unsettled bet"));

        gameTransactionService.markSettled(betTxn.getId(), resultTxn.getSettleTime());

        if (betTxn.hasAliasTxn(round.getClassName())) {
            gameTransactionService.markSettled(betTxn.getRollbackId(round.getClassName()), resultTxn.getSettleTime());
        }

        return betTxn;
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

    private RoundTxn findFirstValidBet(GameRound round) {
        return round.getTransactions().stream()
                .filter(RoundTxn::isSuccessfulBet)
                .filter(t -> !t.isRefunded())
                .findFirst()
                .orElseThrow(() -> new BetNotFoundException(
                        round.getId() + " cannot find valid bet"));
    }

    private PlayerBalanceData callToOperator(BetResultContext context, GameRound round, GameTransaction txn, OperatorBetResultRequest operatorRequest, OperatorScenario scenario) {
        PlayerBalanceData balanceData = operatorApiService.execute(
                betResultOperatorWalletAdapter,
                new OperatorRequestContext<>(
                        operatorRequest,
                        vendorConfigService.getTimeoutInMillis(context.getVendorClassName()),
                        EndPoints.WALLET_BET_RESULT,
                        round,
                        txn,
                        scenario),
                context
        );

        return balanceData.toVendorView(
                round.getUsername(),
                round.getCurrency(),
                context.getToVendorRate()
        );
    }
}
