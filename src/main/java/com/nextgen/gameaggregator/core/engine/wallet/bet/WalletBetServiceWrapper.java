package com.nextgen.gameaggregator.core.engine.wallet.bet;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.exception.translator.WalletExceptionTranslator;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.logging.LogContextService;
import com.nextgen.gameaggregator.core.service.GameSessionDataService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WalletBetServiceWrapper implements WalletBetService {
    private final GameSessionDataService gameSessionDataService;
    private final WalletBetValidator walletBetValidator;
    private final WalletService walletService;
    private final BetResultDataMapper betResultDataMapper;
    private final WalletExceptionTranslator walletExceptionTranslator;

    @Override
    public PlayerBalanceData process(BetContext context) {
        LogContext logContext = LogContextHolder.get();
        HttpRequestLog httpRequestLog = LogContextService.toHttpRequestLog(logContext);

        try {
            walletBetValidator.validateRequestContext(logContext.getVendorClassName(), context);
            GameSession gameSession = retrieveGameSession(context);
            return processBetTransaction(context, gameSession, httpRequestLog);
        } finally {
            LogContextService.updateLogContextFromHttpRequestLog(logContext, httpRequestLog);
        }
    }

    private GameSession retrieveGameSession(BetContext context) {
        GameSession gameSession = gameSessionDataService.getGameSession(context);

        // TODO: Implement support for vendors that don't return session tokens

        walletBetValidator.validateBusinessState(gameSession, context);
        return gameSession;
    }

    private PlayerBalanceData processBetTransaction(
            BetContext context,
            GameSession gameSession,
            HttpRequestLog httpRequestLog) {

        try {
            BetEvent betEvent = walletService.processBet(
                    httpRequestLog.getId(),
                    gameSession,
                    betResultDataMapper.toBetResultData(context),
                    httpRequestLog.getRequestBody(),
                    httpRequestLog
            );

            return PlayerBalanceData.builder()
                    .username(context.getVendorPlayerUsername())
                    .currency(context.getVendorCurrency())
                    .balance(betEvent.getLastBalance())
                    .timestamp(httpRequestLog.getOperatorEnd())
                    .build();

        } catch (Exception ex) {
            walletExceptionTranslator.translateAndThrow(ex);
            return null; // Never reached, but satisfies compiler
        }
    }
}
