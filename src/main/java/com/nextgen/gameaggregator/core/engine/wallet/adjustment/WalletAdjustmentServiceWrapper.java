package com.nextgen.gameaggregator.core.engine.wallet.adjustment;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContextHolder;
import com.nextgen.gameaggregator.core.exception.DuplicateRequestException;
import com.nextgen.gameaggregator.core.exception.translator.WalletExceptionTranslator;
import com.nextgen.gameaggregator.core.idempotency.DuplicateRequestGuard;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.logging.LogContextService;
import com.nextgen.gameaggregator.core.service.GameSessionDataService;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.enums.TxnType;
import com.nextgen.gameaggregator.service.business.GameRoundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletAdjustmentServiceWrapper {
    private static final String LOG_GROUP = "wallet";
    private static final String ACTION = "adjustment";
    private final DuplicateRequestGuard guard;
    private final AdjustmentContextEnricher enricher;
    private final AdjustmentProcessor processor;
    private final GameSessionDataService gameSessionDataService;
    private final GameRoundService gameRoundService;
    private final WalletExceptionTranslator walletExceptionTranslator;

    public PlayerBalanceData process() {
        LogContext logContext = LogContextHolder.get().setLogGroup(LOG_GROUP).setType(ACTION);
        HttpRequestLog httpRequestLog = LogContextService.toHttpRequestLog(logContext);
        String className = logContext.getVendorClassName();
        AdjustmentContext context = state().getAdjustmentContext();
        AdjustmentConfig config = state().getConfig();
        GameTransaction txn = null;

        try {

            txn = guard.ensureNotDuplicate(
                    TxnType.ADJUSTMENT,
                    className,
                    context.getIdempotencyKey(),
                    logContext.getStart()
            );

            GameSession gameSession = gameSessionDataService.getGameSession(context);

            String roundDocId = GameRound.of(className, context.getVendorPlayerUsername(), context.getRoundId()).getId();
            GameRound round = gameRoundService.getOrThrow(roundDocId);

            AdjustmentDecision decision = AdjustmentPolicy.decide(round);
            decision.throwIfRejected(context, config);

            enricher.enrichByGameSession(context, gameSession);

            enricher.enrichGameTransaction(txn, context);

            return processor.processAdjustmentTransaction(context, gameSession, txn, httpRequestLog, state(), round);

        } catch (DuplicateRequestException ex) {
            return handleDuplicateRequest(context, ex);
        } catch (Exception ex) {
            throw handleException(context, txn, ex);
        } finally {
            cleanup();
            LogContextService.updateLogContextFromHttpRequestLog(logContext, httpRequestLog);
        }
    }

    private PlayerBalanceData handleDuplicateRequest(AdjustmentContext context, DuplicateRequestException ex) {
        GameTransaction txn = ex.getTransaction();
        AdjustmentConfig config = state().getConfig();
        if (config.isReturnSuccessOnDuplicate() && txn != null && txn.isSuccess()) {
            return new PlayerBalanceData(
                    context.getVendorPlayerUsername(),
                    context.getVendorCurrency(),
                    Optional.ofNullable(txn.getBalance()).orElse(BigDecimal.ZERO),
                    System.currentTimeMillis()
            );
        }
        throw ex;
    }

    private RuntimeException handleException(AdjustmentContext context, GameTransaction txn, Exception ex) {
        // TODO: if operator error, add to retry queue and return success to vendor

        guard.clear();
        RuntimeException exception = walletExceptionTranslator.translate(ex, context);

        enricher.enrichGameTransactionIfEmpty(txn, context)
                .then(gameRoundService.markTxnErrorAsync(txn, exception))
                .doOnError(t -> log.error("markTxnError failed for txn {}", txn.getId(), t))
                .onErrorComplete() // swallow error, don't terminate subscription
                .subscribe(); // fire and forget, don't block vendor response

        return exception;
    }

    public WalletAdjustmentServiceWrapper initialise(AdjustmentContext context) {
        AdjustmentWrapperContext state = new AdjustmentWrapperContext(context);
        AdjustmentContextHolder.set(state);
        return this;
    }

    public WalletAdjustmentServiceWrapper configure(Consumer<AdjustmentConfig> configurer) {
        configurer.accept(state().getConfig());
        return this;
    }

    private AdjustmentWrapperContext state() {
        return AdjustmentContextHolder.getRequired();
    }

    private void cleanup() {
        guard.cleanup();
        BetResultContextHolder.clear();
    }
}
