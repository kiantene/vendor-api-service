package com.nextgen.gameaggregator.core.engine.wallet.result;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.WalletExceptionTranslator;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.logging.LogContextService;
import com.nextgen.gameaggregator.core.service.GameSessionDataService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WalletBetResultServiceWrapper {
    private static final ThreadLocal<BetResultWrapperContext> stateHolder = new ThreadLocal<>(); // thread safe, context object won't be shared across threads
    private final WalletBetResultValidator validator;
    private final WalletService walletService;
    private final GameSessionDataService gameSessionDataService;
    private final BetResultDataMapper betResultDataMapper;
    private final WalletExceptionTranslator walletExceptionTranslator;

    public PlayerBalanceData process() {
        BetResultContext context = state().getBetResultContext();
        context.setResultTime(System.currentTimeMillis());
        LogContext logContext = LogContextHolder.get();

        validator.validateRequestContext(logContext.getVendorClassName(), context);
        GameSession gameSession = gameSessionDataService.getByVendorToken(context.getVendorSessionToken());

        ResultType resultType = getResultType(context);
        validator.validateBusinessState(gameSession, context, resultType);
        return processBetResultTransaction(context, gameSession, resultType, logContext);
    }

    private PlayerBalanceData processBetResultTransaction(
            BetResultContext context,
            GameSession gameSession,
            ResultType resultType,
            LogContext logContext) {

        HttpRequestLog httpRequestLog = LogContextService.toHttpRequestLog(logContext);
        try {
            BigDecimal balance = walletService.processBetResult(
                    httpRequestLog.getId(),
                    gameSession,
                    betResultDataMapper.toBetResultData(context),
                    resultType,
                    state().getVendorService(),
                    httpRequestLog
            );

            return PlayerBalanceData.builder()
                    .username(context.getVendorPlayerUsername())
                    .currency(context.getVendorCurrency())
                    .balance(balance)
                    .timestamp(httpRequestLog.getOperatorEnd())
                    .build();

        } catch (Exception ex) {
            walletExceptionTranslator.translateAndThrow(ex);
            return null; // Never reached, but satisfies compiler
        } finally {
            cleanup();
            LogContextService.updateLogContextFromHttpRequestLog(logContext, httpRequestLog);
        }
    }

    public WalletBetResultServiceWrapper initialise(BetResultContext context) {
        BetResultWrapperContext state = new BetResultWrapperContext();
        state.setBetResultContext(context);
        stateHolder.set(state);
        return this;
    }

    private BetResultWrapperContext state() {
        BetResultWrapperContext ctx = stateHolder.get();
        if (ctx == null) throw new IllegalStateException("BetResultWrapperContext not initialized");
        return ctx;
    }

    public WalletBetResultServiceWrapper isBetTxn(boolean flag) {
        state().setIsBetTxn(flag);
        return this;
    }

    public WalletBetResultServiceWrapper resultType(ResultType resultType) {
        state().setResultType(resultType);
        return this;
    }

    public WalletBetResultServiceWrapper vendorService(BaseVendorService vendorService) {
        state().setVendorService(vendorService);
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
        if (state().getResultType() != null) return state().getResultType();

        boolean isBet = Optional.ofNullable(state().getIsBetTxn()).orElse(false);
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
        stateHolder.remove();
    }
}
