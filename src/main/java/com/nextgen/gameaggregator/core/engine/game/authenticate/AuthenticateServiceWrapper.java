package com.nextgen.gameaggregator.core.engine.game.authenticate;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.exception.InternalConfigurationException;
import com.nextgen.gameaggregator.core.exception.InternalServerException;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.service.GameSessionDataService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidAgentApiCredentialException;
import com.nextgen.gameaggregator.exception.VendorCurrencyNotSupportException;
import com.nextgen.gameaggregator.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AuthenticateServiceWrapper {
    private final GameSessionDataService gameSessionDataService;
    private final WalletService walletService;

    public PlayerBalanceData process(AuthenticateContext context) {
        LogContext logContext = LogContextHolder.get();
        logContext.setLogGroup("Authenticate");
        GameSession gameSession = gameSessionDataService.getGameSession(context);
        return getBalance(context, gameSession, logContext);
    }

    private HttpRequestLog toHttpRequestLog(LogContext logContext) {
        final Integer PROCESSING = 1;

        HttpRequestLog httpRequestLog = new HttpRequestLog();
        logContext.setTraceId(httpRequestLog.getId());
        httpRequestLog.setUrl(logContext.getUrl());
        httpRequestLog.setRequestBody(logContext.getBody().toString());
        httpRequestLog.setStatus(PROCESSING);
        return httpRequestLog;
    }

    private PlayerBalanceData toPlayerBalanceData(AuthenticateContext context, BigDecimal balance, HttpRequestLog httpRequestLog) {
        PlayerBalanceData playerBalanceData = new PlayerBalanceData();

        playerBalanceData.setUsername(context.getVendorPlayerUsername());
        playerBalanceData.setBalance(balance);
        playerBalanceData.setCurrency(context.getVendorCurrency());
        playerBalanceData.setTimestamp(httpRequestLog.getOperatorEnd());

        return playerBalanceData;
    }

    private void updateLogContext(LogContext logContext, HttpRequestLog httpRequestLog) {
        logContext.setStart(httpRequestLog.getBetStart());
        logContext.setEnd(httpRequestLog.getBetEnd());
        logContext.setApiStart(httpRequestLog.getOperatorStart());
        logContext.setApiEnd(httpRequestLog.getOperatorEnd());
        logContext.put(HttpRequestLog.class.getSimpleName(), httpRequestLog);
    }

    private PlayerBalanceData getBalance(AuthenticateContext context, GameSession gameSession, LogContext logContext) {
        HttpRequestLog httpRequestLog = this.toHttpRequestLog(logContext);
        try {
            BigDecimal balance = walletService.getBalance(httpRequestLog.getId(), gameSession, httpRequestLog);
            return this.toPlayerBalanceData(context, balance, httpRequestLog);

        } catch (InvalidAgentApiCredentialException | VendorCurrencyNotSupportException ex) {

            throw new InternalConfigurationException(ex.getMessage(), ex);
        } catch (Exception ex) {

            throw new InternalServerException(ex.getMessage(), ex);
        } finally {
            this.updateLogContext(logContext, httpRequestLog);
        }
    }
}
