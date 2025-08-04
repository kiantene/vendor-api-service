package com.nextgen.gameaggregator.core.engine.game.authenticate;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.exception.InternalServerException;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AuthenticateServiceWrapper {
    private final GameSessionService gameSessionService;
    private final WalletService walletService;

    public PlayerBalanceData process(AuthenticateContext context) {
        LogContext logContext = LogContextHolder.get();
        logContext.setLogGroup("Authenticate");
        String vendorPlayerUsername = context.getVendorPlayerUsername();
        String vendorSessionToken = context.getVendorSessionToken();
        GameSession gameSession = gameSessionService.getLastGameSessionByVendorPlayerUsername(vendorPlayerUsername);
        // update to use vendor's session token
        gameSessionService.regenerateVendorToken(gameSession, vendorSessionToken);
        HttpRequestLog httpRequestLog = this.toHttpRequestLog(logContext);
        try {
            BigDecimal balance = walletService.getBalance(httpRequestLog.getId(), gameSession, httpRequestLog);
            return this.toPlayerBalanceData(context, balance, httpRequestLog);

        } catch (Exception ex) {
            throw new InternalServerException(ex.getMessage(), ex);
        } finally {
//            this.updateLogContext(logContext, httpRequestLog);
//            httpService.end(httpRequestLog, null);
        }
    }

    private HttpRequestLog toHttpRequestLog(LogContext logContext) {
        final Integer PROCESSING = 1;

        HttpRequestLog httpRequestLog = new HttpRequestLog();
        logContext.setTraceId(httpRequestLog.getId());
        httpRequestLog.setUrl(logContext.getUrl());
        httpRequestLog.setRequestBody(logContext.getBody());
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
}
