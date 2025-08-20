package com.nextgen.gameaggregator.core.engine.wallet.bet;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.exception.translator.WalletExceptionTranslator;
import com.nextgen.gameaggregator.core.logging.*;
import com.nextgen.gameaggregator.core.service.GameSessionDataService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.WalletService;
import jakarta.validation.constraints.NotNull;
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
    public PlayerBalanceData process(@NotNull BetContext context) {
        LogContext logContext = LogContextHolder.get();
        logContext.setLogGroup("Bet");
        HttpRequestLog httpRequestLog = LogContextService.toHttpRequestLog(logContext);

        try {
            walletBetValidator.validateRequestContext(logContext.getVendorClassName(), context);
            GameSession gameSession = gameSessionDataService.getGameSession(context);
            walletBetValidator.validateBusinessState(gameSession, context);
            return processBetTransaction(context, gameSession, httpRequestLog);
        } catch (Exception ex) {
            walletExceptionTranslator.translateAndThrow(ex);
            return null; // Never reached, but satisfies compiler
        } finally {
            LogContextService.updateLogContextFromHttpRequestLog(logContext, httpRequestLog);
        }
    }

    private PlayerBalanceData processBetTransaction(
            BetContext context,
            GameSession gameSession,
            HttpRequestLog httpRequestLog) throws
                InvalidAgentApiCredentialException, VendorCurrencyNotSupportException,
                BetResultIdempotentViolationException, InsufficientBalanceException,
                TransactionStillProcessingException, InvalidOperatorResponseException,
                CouchbaseDataIntegrityException {

        BetEvent betEvent = walletService.processBet(
                httpRequestLog.getId(),
                gameSession,
                betResultDataMapper.toBetResultData(context),
                httpRequestLog.getRequestBody(),
                httpRequestLog
        );

        return new PlayerBalanceData(
                context.getVendorPlayerUsername(),
                context.getVendorCurrency(),
                betEvent.getLastBalance(),
                httpRequestLog.getOperatorEnd()
        );
    }
}
