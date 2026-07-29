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
import com.nextgen.gameaggregator.service.business.maxpayout.AgentMaxPayoutService;
import com.nextgen.gameaggregator.service.business.maxpayout.CapRequest;
import com.nextgen.gameaggregator.service.business.maxpayout.ResultAmounts;
import com.nextgen.gameaggregator.service.data.producer.BetHistoryProducer;
import com.nextgen.gameaggregator.service.data.producer.endround.RoundEndedTriggerProducer;
import com.nextgen.gameaggregator.service.data.producer.transactionhistory.BetTransactionHistoryProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.parameters.P;
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
    private final RoundEndedTriggerProducer roundEndedTriggerProducer;
    private final AgentMaxPayoutService agentMaxPayoutService;

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

        /**
         * Apply the agent max-payout cap on win-bearing results (the txn that credits the player).
         * {@link ResultType#isWin()} covers WIN and BET_WIN so the combined bet+result path is not
         * missed. Records the capped amounts on resultTxn so markSuccess persists them to the round
         * slice, from which bet history (and the SettleByRound round-end summary) is derived.
         */
        if (resultType.isWin()) {
            applyPayoutCapToResult(context, round, resultTxn);
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
                SettleByRoundScenario scenario = new SettleByRoundScenario(resultType, betFullTxn);

                OperatorBetResultRequest operatorRequest =
                        settleByRoundOperatorBetResultRequestMapper.toOperatorRequest(
                                OperatorApiContext.of(context, round, resultTxn),
                                scenario
                        );

                balanceData = callToOperator(context, round, resultTxn, operatorRequest, scenario);
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

    /**
     * Cap the WIN result per the agent max-payout config and stash the capped amounts on the txn
     * (vendor units). Each capped field is set ONLY when it is strictly lower than the vendor amount,
     * so a non-null {@code cappedWinAmount} always means "win was actually reduced" (and likewise for
     * jackpot). Readers coalesce a null capped field back to the uncapped vendor amount.
     */
    private void applyPayoutCapToResult(BetResultContext context, GameRound round, GameTransaction resultTxn) {
        ResultAmounts capped = agentMaxPayoutService.applyPayoutCap(
                new CapRequest(
                        round.getAgentMeta().getAgentId(),
                        context.getVendorId(),
                        context.getGameCategoryId(),
                        context.getCurrencyId(),
                        resultTxn.getBetAmount(),
                        resultTxn.getWinAmount(),
                        resultTxn.getJackpotAmount()
                ),
                context.getToVendorRate()
        );

        if (!capped.capped()) return;

        // Set each field only when it was genuinely reduced (e.g. a jackpot-only cap must not
        // stamp cappedWinAmount == winAmount, which would read as "win reduced" downstream).
        if (isReduced(capped.cappedWin(), resultTxn.getWinAmount())) {
            resultTxn.setCappedWinAmount(capped.cappedWin());
        }
        if (isReduced(capped.cappedJackpot(), resultTxn.getJackpotAmount())) {
            resultTxn.setCappedJackpotAmount(capped.cappedJackpot());
        }
    }

    private static boolean isReduced(BigDecimal capped, BigDecimal vendor) {
        return capped != null && vendor != null && capped.compareTo(vendor) < 0;
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

            /**
             * If Result after RoundEnded is Possible, we will Publish to a Temp Staging Table
             * A seperate Scheduler will pick up and send the LATEST one for each round to Bet History table
             */
            boolean publishToCoalescingTable = config.isAllowResultWhenRoundHasEnded();
//            betHistoryProducer.publishBetHistoryByRound(context, updatedRound, resultTxn, betFullTxn);

            /**
             *  If using Legacy Wallet Service, it will already have its own EndRoundProcess logic
             *  New Framework EndRoundService will handle the Bet History Publishing
             */
            if (!vendorConfigService.isWalletServiceLegacyEnabled(context.getVendorClassName())) {
                roundEndedTriggerProducer.publishEndRound(round);
            } else {
                betHistoryProducer.publishBetHistoryByRound(context, updatedRound, resultTxn, betFullTxn);
            }

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

        // Cap the win portion of the combined bet+result. This path's ResultType is BET_WIN, which
        // ResultType.isWin() covers (a bare == WIN check would wrongly skip it).
        if (resultType.isWin()) {
            applyPayoutCapToResult(context, round, txn);
        }

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
