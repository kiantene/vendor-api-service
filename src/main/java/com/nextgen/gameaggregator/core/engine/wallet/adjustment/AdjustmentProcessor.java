package com.nextgen.gameaggregator.core.engine.wallet.adjustment;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.service.WalletLegacyService;
import com.nextgen.gameaggregator.entity.couchbase.AgentMeta;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.entity.couchbase.RoundTxn;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.business.GameTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
class AdjustmentProcessor {
    private final GameTransactionService gameTransactionService;
    private final WalletLegacyService walletLegacyService;

    public PlayerBalanceData processAdjustmentTransaction(
            AdjustmentContext context,
            GameSession gameSession,
            GameTransaction adjustmentTxn,
            HttpRequestLog httpRequestLog,
            AdjustmentWrapperContext state,
            GameRound round) throws InvalidAgentApiCredentialException, VendorCurrencyNotSupportException, InsufficientBalanceException, TransactionStillProcessingException, BetNotFoundException, SettledBetNotFoundException, InvalidOperatorResponseException, BetAdjustmentIdempotentViolationException {
        AdjustmentConfig config = state.getConfig();

        onBeforeSendAdjustment(gameSession, adjustmentTxn, context, round, httpRequestLog, config);

        PlayerBalanceData balanceData = walletLegacyService.processAdjustment(httpRequestLog, gameSession, state, adjustmentTxn);

        onAfterSendAdjustment(round, adjustmentTxn, balanceData.getBalance(), context);

        return balanceData;
    }

    private void onBeforeSendAdjustment(GameSession gameSession, GameTransaction adjustmentTxn, AdjustmentContext context, GameRound round, HttpRequestLog httpRequestLog, AdjustmentConfig config) {

        RoundTxn resultRoundTxn = findResult(round, adjustmentTxn.getVendorBetId());
        if (config.isCalculateAdjustmentAmount()) {
            // Current Framework setup is unable to handle multiple adjustment, therefore will not be able to find last known win amount.
            GameTransaction resultTxn = gameTransactionService.getOrThrow(resultRoundTxn.getId());
            context.setAdjustmentAmount(context.getWinAmount().subtract(resultTxn.getWinAmount()));
        }

        // Update txn status to sent
        gameTransactionService.markSent(adjustmentTxn, AgentMeta.ofGameSession(gameSession));

        httpRequestLog.setGaBetId(resultRoundTxn.getGaBetId());
    }

    private void onAfterSendAdjustment(GameRound round, GameTransaction adjustmentTxn, BigDecimal balance, AdjustmentContext context) {

        // we save the original balance from operator
        gameTransactionService.markSuccess(round, adjustmentTxn, balance, true);
    }

    private RoundTxn findResult(GameRound round, String vendorBetId) {
        return round.getTransactions().stream()
                .filter(RoundTxn::isSuccessfulResult)
                .filter(txn -> txn.getVendorBetId().equals(vendorBetId))
                .filter(txn -> !txn.isRefunded())
                .findFirst()
                .orElseThrow(() -> new com.nextgen.gameaggregator.core.exception.BetResultNotFoundException(
                        round.getId() + " cannot find valid result"));
    }
}
