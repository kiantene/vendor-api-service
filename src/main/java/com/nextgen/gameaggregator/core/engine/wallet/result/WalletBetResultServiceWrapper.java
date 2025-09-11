package com.nextgen.gameaggregator.core.engine.wallet.result;

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
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.service.business.GameTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class WalletBetResultServiceWrapper {
    private static final String LOG_GROUP = "wallet";
    private static final String ACTION = "result";
    private final DuplicateRequestGuard guard;
    private final BetResultContextEnricher enricher;
    private final BetResultDataMapper betResultDataMapper;
    private final GameSessionDataService gameSessionDataService;
    private final GameTransactionService gameTransactionService;
    private final WalletBetResultValidator validator;
    private final WalletExceptionTranslator walletExceptionTranslator;
    private final WalletService walletService;

    public PlayerBalanceData process() {
        LogContext logContext = LogContextHolder.get().setLogGroup(LOG_GROUP).setType(ACTION);
        HttpRequestLog httpRequestLog = LogContextService.toHttpRequestLog(logContext);
        BetResultContext context = state().getBetResultContext();
        GameTransaction txn = null;

        try {
            txn = guard.ensureNotDuplicate(
                    TxnType.RESULT,
                    logContext.getVendorClassName(),
                    context.getIdempotencyKey(),
                    logContext.getStart()
            );

            GameSession gameSession = gameSessionDataService.getOrCreate(context);

            enricher.enrichByGameSession(context, gameSession, state().getConfig());

            ResultType resultType = getResultType(context);

            validator.validateBusinessState(gameSession, context, resultType);

            return processBetResultTransaction(context, gameSession, txn, resultType, httpRequestLog);

        } catch (DuplicateRequestException ex) {
            return handleDuplicateRequest(context, ex);
        } catch (Exception ex) {
            // TODO: handle BetNotFoundException (race condition)

            guard.clear();
            RuntimeException translatedEx = walletExceptionTranslator.translate(ex);
            gameTransactionService.markError(txn, translatedEx);

            throw translatedEx;
        } finally {
            cleanup();
            LogContextService.updateLogContextFromHttpRequestLog(logContext, httpRequestLog);
        }
    }

    private PlayerBalanceData handleDuplicateRequest(BetResultContext context, DuplicateRequestException ex) {
        // TODO: check for operator status, if is successful then return success
        throw ex;
    }

    private PlayerBalanceData processBetResultTransaction(
            BetResultContext context,
            GameSession gameSession,
            GameTransaction txn,
            ResultType resultType,
            HttpRequestLog httpRequestLog) throws
                InvalidAgentApiCredentialException, VendorCurrencyNotSupportException,
                BetResultIdempotentViolationException, MergedBetDataIntegrityException,
                InsufficientBalanceException, TransactionStillProcessingException,
                BetNotFoundException, InvalidOperatorResponseException, InternalServerTimeoutRetryException {

        enricher.enrichGameTransaction(txn, context);
        gameTransactionService.markSent(txn, buildAgentMeta(context, gameSession));
        BigDecimal balance = walletService.processBetResult(
                httpRequestLog.getId(),
                gameSession,
                betResultDataMapper.toBetResultData(context),
                resultType,
                state().getVendorService(),
                httpRequestLog
        );
        txn.setGaBetId(httpRequestLog.getGaBetId());
        gameTransactionService.markSuccess(txn, balance, context.getRoundEnded());

        return new PlayerBalanceData(
                context.getVendorPlayerUsername(),
                context.getVendorCurrency(),
                balance,
                httpRequestLog.getOperatorEnd()
        );
    }

    public WalletBetResultServiceWrapper initialise(BetResultContext context) {
        BetResultWrapperContext state = new BetResultWrapperContext(context);
        BetResultContextHolder.set(state);
        return this;
    }

    private BetResultWrapperContext state() {
        return BetResultContextHolder.getRequired();
    }

    public WalletBetResultServiceWrapper configure(Consumer<BetResultConfig> configurer) {
        configurer.accept(state().getConfig());
        return this;
    }

    /**
     * Scenarios:
     * 1. WIN        -> Win transaction for a previous bet (not a bet)
     * 2. BET_WIN    -> Bet with win or jackpot
     * 3. BET_LOSE   -> Bet with no win
     * 4. END        -> Non-bet transaction with no win (default fallback)
     */
    private ResultType getResultType(BetResultContext context) {
        BetResultConfig config = state().getConfig();
        if (config.getResultType() != null) return config.getResultType();

        boolean isBet = config.isBetTxn();
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

    private void cleanup() {
        guard.cleanup();
        BetResultContextHolder.clear();
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
