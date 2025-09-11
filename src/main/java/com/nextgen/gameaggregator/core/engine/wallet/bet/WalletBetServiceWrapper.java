package com.nextgen.gameaggregator.core.engine.wallet.bet;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.exception.DuplicateRequestException;
import com.nextgen.gameaggregator.core.exception.translator.WalletExceptionTranslator;
import com.nextgen.gameaggregator.core.idempotency.DuplicateRequestGuard;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.logging.LogContextService;
import com.nextgen.gameaggregator.core.service.GameSessionDataService;
import com.nextgen.gameaggregator.entity.couchbase.AgentMeta;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.enums.TxnType;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.service.business.GameTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class WalletBetServiceWrapper implements WalletBetService {
    private static final String LOG_GROUP = "wallet";
    private static final String ACTION = "bet";
    private final DuplicateRequestGuard guard;
    private final BetContextEnricher enricher;
    private final BetResultDataMapper betResultDataMapper;
    private final GameSessionDataService gameSessionDataService;
    private final GameTransactionService gameTransactionService;
    private final WalletBetValidator walletBetValidator;
    private final WalletService walletService;
    private final WalletExceptionTranslator walletExceptionTranslator;

    @Override
    public PlayerBalanceData process() {
        return process(state().getBetContext());
    }

    @Override
    public PlayerBalanceData process(BetContext context) {
        LogContext logContext = LogContextHolder.get().setLogGroup(LOG_GROUP).setType(ACTION);
        HttpRequestLog httpRequestLog = LogContextService.toHttpRequestLog(logContext);
        GameTransaction txn = null;

        try {
            txn = guard.ensureNotDuplicate(
                    TxnType.BET,
                    logContext.getVendorClassName(),
                    context.getIdempotencyKey(),
                    logContext.getStart()
            );

            GameSession gameSession = gameSessionDataService.getGameSession(context);

            enricher.enrichByGameSession(context, gameSession);

            walletBetValidator.validateBusinessState(gameSession, context);

            return processBetTransaction(context, gameSession, txn, httpRequestLog);

        } catch (DuplicateRequestException ex) {
            return handleDuplicateRequest(context, ex);
        } catch (Exception ex) {
            guard.clear();
            RuntimeException translatedEx = walletExceptionTranslator.translate(ex);
            gameTransactionService.markError(txn, translatedEx);

            throw translatedEx;
        } finally {
            cleanup();
            LogContextService.updateLogContextFromHttpRequestLog(logContext, httpRequestLog);
        }
    }

    @Override
    public WalletBetService initialise(BetContext context) {
        BetWrapperContext state = new BetWrapperContext(context);
        BetContextHolder.set(state);
        return this;
    }

    @Override
    public WalletBetService configure(Consumer<BetConfig> configurer) {
        configurer.accept(state().getConfig());
        return this;
    }

    private BetWrapperContext state() {
        return BetContextHolder.getRequired();
    }

    private void cleanup() {
        guard.cleanup();
        BetContextHolder.clear();
    }

    private PlayerBalanceData handleDuplicateRequest(BetContext context, DuplicateRequestException ex) {
        GameTransaction txn = ex.getTransaction();
        if (state().getConfig().isReturnSuccessOnDuplicate() && txn.isSuccess()) {
            return new PlayerBalanceData(
                    txn.getUsername(),
                    txn.getCurrency(),
                    txn.getBalance(),
                    System.currentTimeMillis()
            );
        }
        throw ex;
    }

    private PlayerBalanceData processBetTransaction(
            BetContext context,
            GameSession gameSession,
            GameTransaction txn,
            HttpRequestLog httpRequestLog) throws
                InvalidAgentApiCredentialException, VendorCurrencyNotSupportException,
                BetResultIdempotentViolationException, InsufficientBalanceException,
                TransactionStillProcessingException, InvalidOperatorResponseException,
                CouchbaseDataIntegrityException {

        enricher.enrichGameTransaction(txn, context);
        gameTransactionService.markSent(txn, buildAgentMeta(context, gameSession));
        BetEvent betEvent = walletService.processBet(
                httpRequestLog.getId(),
                gameSession,
                betResultDataMapper.toBetResultData(context),
                httpRequestLog.getRequestBody(),
                httpRequestLog
        );
        txn.setGaBetId(betEvent.getBetInformation().getBetId());
        gameTransactionService.markSuccess(txn, betEvent.getLastBalance());

        return new PlayerBalanceData(
                context.getVendorPlayerUsername(),
                context.getVendorCurrency(),
                betEvent.getLastBalance(),
                httpRequestLog.getOperatorEnd()
        );
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
