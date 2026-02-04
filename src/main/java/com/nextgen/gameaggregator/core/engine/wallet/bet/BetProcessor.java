package com.nextgen.gameaggregator.core.engine.wallet.bet;

import com.nextgen.gameaggregator.core.context.OperatorRequestContext;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.operator.BetScenario;
import com.nextgen.gameaggregator.core.engine.operator.OperatorApiContext;
import com.nextgen.gameaggregator.core.engine.operator.OperatorApiService;
import com.nextgen.gameaggregator.core.engine.operator.wallet.bet.BetOperatorWalletAdapter;
import com.nextgen.gameaggregator.core.engine.operator.wallet.bet.OperatorBetRequest;
import com.nextgen.gameaggregator.core.engine.operator.wallet.bet.OperatorBetRequestMapper;
import com.nextgen.gameaggregator.core.service.WalletLegacyService;
import com.nextgen.gameaggregator.core.vendor.config.VendorConfigService;
import com.nextgen.gameaggregator.entity.couchbase.AgentMeta;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.operator.wallet.bet.WalletBetDto;
import com.nextgen.gameaggregator.service.business.GameRoundService;
import com.nextgen.gameaggregator.service.business.GameTransactionService;
import com.nextgen.gameaggregator.service.data.model.TxnAmount;
import com.nextgen.gameaggregator.service.data.producer.transactionhistory.BetTransactionHistoryProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BetProcessor {

    private final BetContextEnricher enricher;
    private final GameTransactionService gameTransactionService;
    private final GameRoundService gameRoundService;
    private final VendorConfigService vendorConfigService;
    private final WalletLegacyService walletLegacyService;
    private final OperatorBetRequestMapper operatorBetRequestMapper;
    private final BetOperatorWalletAdapter betOperatorWalletAdapter;
    private final OperatorApiService operatorApiService;
    private final BetTransactionHistoryProducer betTransactionHistoryProducer;

    public PlayerBalanceData processBetTransaction(
            BetWrapperContext betWrapperContext,
            GameSession gameSession,
            GameTransaction txn,
            HttpRequestLog httpRequestLog) throws
            InvalidAgentApiCredentialException, VendorCurrencyNotSupportException,
            BetResultIdempotentViolationException, InsufficientBalanceException,
            TransactionStillProcessingException, InvalidOperatorResponseException,
            CouchbaseDataIntegrityException {

        BetContext context = betWrapperContext.getBetContext();

        GameRound round = onBeforeSendBet(betWrapperContext, gameSession, txn);

        PlayerBalanceData balanceData;

        if (vendorConfigService.isWalletServiceLegacyEnabled(context.getVendorClassName())) {
            balanceData = walletLegacyService.processBet(httpRequestLog, gameSession, context, txn);
        } else {
            BetScenario scenario = new BetScenario();

            OperatorBetRequest operatorRequest = operatorBetRequestMapper.toOperatorRequest(
                    OperatorApiContext.of(context, round, txn),
                    scenario
            );

            balanceData = operatorApiService.execute(
                    betOperatorWalletAdapter,
                    new OperatorRequestContext<>(
                            operatorRequest,
                            vendorConfigService.getTimeoutInMillis(context.getVendorClassName()),
                            EndPoints.WALLET_BET,
                            round,
                            txn,
                            scenario),
                    context
            );

            balanceData = balanceData.toVendorView(
                    round.getUsername(),
                    round.getCurrency(),
                    context.getToVendorRate()
            );
        }

        onAfterSendBet(round, txn, balanceData.getBalance(), context);

        return balanceData;
    }

    private GameRound onBeforeSendBet(BetWrapperContext betWrapperContext, GameSession gameSession, GameTransaction txn) {
        BetConfig config = betWrapperContext.getConfig();
        BetContext context = betWrapperContext.getBetContext();

        String roundDocId = GameRound.of(txn.getClassName(), context.getVendorPlayerUsername(), context.getRoundId()).getId();
        Optional<GameRound> roundOpt = gameRoundService.get(roundDocId);

        BetDecision decision = BetPolicy.decide(roundOpt, config);
        decision.throwIfRejected(context, config);

        enricher.enrichGameTransaction(txn, context);
        GameRound round = gameTransactionService.markSent(txn, buildAgentMeta(context, gameSession));

        return round;
    }

    private void onAfterSendBet(GameRound round, GameTransaction txn, BigDecimal balance, BetContext context) {
        gameTransactionService.markSuccess(round, txn, balance);

        // Send Kafka Message For Transaction History
        if (vendorConfigService.isTransactionHistoryEnabled(context.getVendorClassName())) {
            betTransactionHistoryProducer.publishTransactionHistoryForBet(context, round, txn);
        }
    }

    private AgentMeta buildAgentMeta(BetContext context, GameSession gameSession) {
        AgentMeta agentMeta = new AgentMeta();
        agentMeta.setAgentId(context.getAgentId());
        agentMeta.setUsername(context.getAgentPlayerUsername());
        agentMeta.setCurrency(context.getCurrencyCode());
        agentMeta.setGameCode(context.getGameCode());
        agentMeta.setSession(gameSession.getToken());

        return agentMeta;
    }
}
