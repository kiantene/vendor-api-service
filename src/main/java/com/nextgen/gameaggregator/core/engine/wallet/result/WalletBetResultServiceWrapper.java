package com.nextgen.gameaggregator.core.engine.wallet.result;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.BetTransaction;
import com.nextgen.gameaggregator.core.exception.translator.WalletExceptionTranslator;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.logging.LogContextService;
import com.nextgen.gameaggregator.core.service.GameSessionDataService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class WalletBetResultServiceWrapper {
    private final BetResultContextEnricher enricher;
    private final BetResultDataMapper betResultDataMapper;
    private final GameSessionDataService gameSessionDataService;
    private final WalletBetResultValidator validator;
    private final WalletBetResultBatchService batchService;
    private final WalletExceptionTranslator walletExceptionTranslator;
    private final WalletService walletService;

    public PlayerBalanceData process() {
        LogContext logContext = LogContextHolder.get();
        logContext.setLogGroup("Result");
        HttpRequestLog httpRequestLog = LogContextService.toHttpRequestLog(logContext);

        try {
            BetResultContext context = state().getBetResultContext();
            enricher.enrich(context);

            validator.validateRequestContext(logContext.getVendorClassName(), context);
            GameSession gameSession = gameSessionDataService.getOrCreate(context);

            ResultType resultType = getResultType(context);
            validator.validateBusinessState(gameSession, context, resultType);
            PlayerBalanceData playerBalance = processBetResultTransaction(context, gameSession, resultType, httpRequestLog);
            doProcessBatch(context);

            return playerBalance;
        } catch (Exception ex) {
            // TODO: clear request idempotent only when operator response is successful
            validator.clearRequestIdempotent();
            walletExceptionTranslator.translateAndThrow(ex);
        } finally {
            cleanup();
            LogContextService.updateLogContextFromHttpRequestLog(logContext, httpRequestLog);
        }
        return null;
    }

    private void doProcessBatch(BetResultContext context) {
        BetResultConfig config = state().getConfig();

        if (config.getProcessingMode().isBatchMode()) {
            List<BetTransaction> txnList = context.getBetTransactions();

            if (txnList == null || txnList.isEmpty()) return;

            batchService.processBatch(txnList, context);
        }
    }

    private PlayerBalanceData processBetResultTransaction(
            BetResultContext context,
            GameSession gameSession,
            ResultType resultType,
            HttpRequestLog httpRequestLog) throws
                InvalidAgentApiCredentialException, VendorCurrencyNotSupportException,
                BetResultIdempotentViolationException, MergedBetDataIntegrityException,
                InsufficientBalanceException, TransactionStillProcessingException,
                BetNotFoundException, InvalidOperatorResponseException, InternalServerTimeoutRetryException {

        BigDecimal balance = walletService.processBetResult(
                httpRequestLog.getId(),
                gameSession,
                betResultDataMapper.toBetResultData(context),
                resultType,
                state().getVendorService(),
                httpRequestLog
        );

        return new PlayerBalanceData(
                context.getVendorPlayerUsername(),
                context.getVendorCurrency(),
                balance,
                httpRequestLog.getOperatorEnd()
        );
    }

    public WalletBetResultServiceWrapper initialise(BetResultContext context) {
        BetResultWrapperContext state = new BetResultWrapperContext();
        state.setBetResultContext(context);
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
        BetResultContextHolder.clear();
    }
}
