package com.nextgen.gameaggregator.core.engine.wallet.result;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
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
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.business.GameRoundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletBetResultServiceWrapper {
    private static final String LOG_GROUP = "wallet";
    private static final String ACTION = "result";
    private final DuplicateRequestGuard guard;
    private final BetResultContextEnricher enricher;
    private final BetResultProcessor processor;
    private final GameSessionDataService gameSessionDataService;
    private final GameRoundService gameRoundService;
    private final WalletBetResultValidator validator;
    private final WalletExceptionTranslator walletExceptionTranslator;

    public PlayerBalanceData process() {
        LogContext logContext = LogContextHolder.get().setLogGroup(LOG_GROUP).setType(ACTION);
        HttpRequestLog httpRequestLog = LogContextService.toHttpRequestLog(logContext);
        String className = logContext.getVendorClassName();
        BetResultContext context = state().getBetResultContext();
        BetResultConfig config = state().getConfig();
        GameTransaction txn = null;

        try {
            validator.validateRequestContext(context, state().getConfig());

            txn = guard.ensureNotDuplicate(
                    config.isBetAndResult() ? TxnType.BET_N_RESULT : TxnType.RESULT,
                    className,
                    context.getIdempotencyKey(),
                    logContext.getStart()
            );

            Optional<GameRound> roundOpt = gameRoundService.get(GameRound.of(className, context.getRoundId()).getId());

            BetResultDecision decision = BetResultPolicy.decide(roundOpt, config);
            decision.throwIfRejected(context, config);

            GameSession gameSession = gameSessionDataService.getOrCreate(context);

            enricher.enrichByGameSession(context, gameSession, config);

            ResultType resultType = getResultType(context, config);

            validator.validateBusinessState(gameSession, context, resultType);

            enricher.enrichGameTransaction(txn, context);

            if (config.isBetAndResult()) {
                return processor.processBetAndResultTransaction(context, gameSession, txn, resultType, httpRequestLog, state());
            }

            return processor.processResultTransaction(context, gameSession, txn, resultType, httpRequestLog, state());

        } catch (DuplicateRequestException ex) {
            return handleDuplicateRequest(context, ex);
        } catch (Exception ex) {
            throw handleException(context, txn, ex);
        } finally {
            cleanup();
            LogContextService.updateLogContextFromHttpRequestLog(logContext, httpRequestLog);
        }
    }

    private PlayerBalanceData handleDuplicateRequest(BetResultContext context, DuplicateRequestException ex) {
        GameTransaction txn = ex.getTransaction();
        BetResultConfig config = state().getConfig();
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

    private RuntimeException handleException(BetResultContext context, GameTransaction txn, Exception ex) {
        // TODO: if operator error, add to retry queue and return success to vendor

        guard.clear();
        RuntimeException exception = walletExceptionTranslator.translate(ex, context);

        markTxnError(context, txn, exception)
                .doOnError(t -> log.warn("markTxnError failed for txn {}", txn.getId(), t))
                .subscribe(); // fire and forget, don't block vendor response

        return exception;
    }

    private Mono<Void> markTxnError(BetResultContext context, GameTransaction txn, RuntimeException ex) {
        return enricher.enrichGameTransactionIfEmpty(txn, context)
                .then(Mono.<Void>fromRunnable(
                        () -> gameRoundService.markTxnError(txn, ex))   // this is a blocking call
                        .subscribeOn(Schedulers.boundedElastic())       // use a separate thread to handle blocking
                );
    }

    public WalletBetResultServiceWrapper initialise(BetResultContext context) {
        BetResultWrapperContext state = new BetResultWrapperContext(context);
        BetResultContextHolder.set(state);
        return this;
    }

    public WalletBetResultServiceWrapper configure(Consumer<BetResultConfig> configurer) {
        configurer.accept(state().getConfig());
        return this;
    }

    private BetResultWrapperContext state() {
        return BetResultContextHolder.getRequired();
    }

    private void cleanup() {
        guard.cleanup();
        BetResultContextHolder.clear();
    }

    /**
     * Scenarios:
     * 1. WIN        -> Win transaction for a previous bet (not a bet)
     * 2. BET_WIN    -> Bet with win or jackpot
     * 3. BET_LOSE   -> Bet with no win
     * 4. END        -> Non-bet transaction with no win (default fallback)
     */
    private ResultType getResultType(BetResultContext context, BetResultConfig config) {
        if (config.getResultType() != null) return config.getResultType();

        boolean isBet = config.isBetAndResult();
        BigDecimal winAmount = Optional.ofNullable(context.getWinAmount()).orElse(BigDecimal.ZERO);
        BigDecimal jackpotAmount = Optional.ofNullable(context.getJackpotAmount()).orElse(BigDecimal.ZERO);
        boolean hasWin = winAmount.compareTo(BigDecimal.ZERO) > 0;
        boolean hasJackpot = jackpotAmount.compareTo(BigDecimal.ZERO) > 0;

        if (isBet) {
            return (hasWin || hasJackpot) ? ResultType.BET_WIN : ResultType.BET_LOSE;
        } else {
            return (hasWin || hasJackpot) ? ResultType.WIN : ResultType.END;
        }
    }
}
