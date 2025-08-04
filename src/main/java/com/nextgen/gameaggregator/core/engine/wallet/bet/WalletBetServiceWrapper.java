package com.nextgen.gameaggregator.core.engine.wallet.bet;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.exception.DuplicateBetException;
import com.nextgen.gameaggregator.core.exception.InternalConfigurationException;
import com.nextgen.gameaggregator.core.exception.InternalServerException;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.service.GameSessionDataService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WalletBetServiceWrapper implements WalletBetService {
    private final GameSessionDataService gameSessionDataService;
    private final WalletBetValidator walletBetValidator;
    private final WalletService walletService;
    private final HttpService httpService;
    private final BetResultDataMapper betResultDataMapper;

    @Override
    public PlayerBalanceData process(BetContext context) {
        LogContext logContext = LogContextHolder.get();
        final String vendorClassName = logContext.getVendorClassName();

        walletBetValidator.validateRequestContext(vendorClassName, context);
        GameSession gameSession = gameSessionDataService.getByVendorToken(context.getVendorSessionToken());
        // TODO: need to add support for vendor that does not return session token
        walletBetValidator.validateBusinessState(gameSession, context);

        BetResultData betResultData = betResultDataMapper.toBetResultData(context);
        HttpRequestLog httpRequestLog = this.toHttpRequestLog(logContext);

        try {
            BetEvent betEvent = walletService.processBet(
                    httpRequestLog.getId(),
                    gameSession,
                    betResultData,
                    logContext.getBody(),
                    httpRequestLog
            );

            return this.toPlayerBalanceData(context, betEvent, httpRequestLog);

        } catch (InvalidAgentApiCredentialException | VendorCurrencyNotSupportException ex) {

            throw new InternalConfigurationException(ex.getMessage(), ex);
        } catch (CouchbaseDataIntegrityException | InvalidOperatorResponseException ex) {

            throw new InternalServerException(ex.getMessage(), ex);
        } catch (BetResultIdempotentViolationException ex) {

            throw new DuplicateBetException(ex.getBetId());
        } catch (TransactionStillProcessingException ex) {

            throw new DuplicateBetException(ex.getMessage());
        } catch (InsufficientBalanceException ex) {

            throw new com.nextgen.gameaggregator.core.exception.InsufficientBalanceException();
        } finally {
            this.updateLogContext(logContext, httpRequestLog);
            httpService.end(httpRequestLog, null);
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

    private PlayerBalanceData toPlayerBalanceData(BetContext context, BetEvent betEvent, HttpRequestLog httpRequestLog) {
        PlayerBalanceData playerBalanceData = new PlayerBalanceData();

        playerBalanceData.setUsername(context.getVendorPlayerUsername());
        playerBalanceData.setBalance(betEvent.getLastBalance());
        playerBalanceData.setCurrency(context.getVendorCurrency());
        playerBalanceData.setTimestamp(httpRequestLog.getOperatorEnd()); // as we do not have this info, default to operatorEnd

        return playerBalanceData;
    }

    private void updateLogContext(LogContext logContext, HttpRequestLog httpRequestLog) {
        logContext.setStart(httpRequestLog.getBetStart());
        logContext.setEnd(httpRequestLog.getBetEnd());
        logContext.setApiStart(httpRequestLog.getOperatorStart());
        logContext.setApiEnd(httpRequestLog.getOperatorEnd());
    }
}
