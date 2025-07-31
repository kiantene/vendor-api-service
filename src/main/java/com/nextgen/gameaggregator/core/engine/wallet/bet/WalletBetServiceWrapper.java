package com.nextgen.gameaggregator.core.engine.wallet.bet;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.exception.DuplicateBetException;
import com.nextgen.gameaggregator.core.exception.InternalConfigurationException;
import com.nextgen.gameaggregator.core.exception.InternalServerException;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.service.GameSessionDataService;
import com.nextgen.gameaggregator.core.validator.WalletBetValidator;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WalletBetServiceWrapper implements WalletBetService {
    private final GameSessionDataService gameSessionDataService;
    private final WalletBetValidator walletBetValidator;
    private final WalletService walletService;

    @Override
    public PlayerBalanceData process(BetContext context) {
        LogContext logContext = LogContextHolder.get();
        final String vendorClassName = logContext.getVendorClassName();

        walletBetValidator.validatePreSession(vendorClassName, context);
        GameSession gameSession = gameSessionDataService.getByVendorToken(context.getVendorSessionToken());
        // TODO: need to add support for vendor that does not return session token
        walletBetValidator.validateOrThrow(gameSession, context);

        BetResultData betResultData = this.toBetResultData(context);
        HttpRequestLog httpRequestLog = this.toHttpRequestLog(logContext);

        try {
            BetEvent betEvent = walletService.processBet(context.getTraceId(), gameSession, betResultData, logContext.getBody(), httpRequestLog);

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
        }
    }

    private BetResultData toBetResultData(BetContext betContext) {
        return new BetResultData() {
            @Override
            public String getExternalTransactionId() {
                return betContext.getIdempotencyKey();
            }

            @Override
            public String getVendorBetId() {
                return betContext.getVendorBetId();
            }

            @Override
            public String getRoundId() {
                return betContext.getRoundId();
            }

            @Override
            public String getGameId() {
                return betContext.getGameCode();
            }

            @Override
            public BigDecimal getBetAmount() {
                return betContext.getBetAmount();
            }

            @Override
            public BigDecimal getWinAmount() {
                return BigDecimal.ZERO;
            }

            @Override
            public BigDecimal getWinLoss() {
                return BigDecimal.ZERO;
            }

            @Override
            public BigDecimal getEffectiveTurnover() {
                return betContext.getBetAmount();
            }

            @Override
            public Long getVendorBetTime() {
                return betContext.getTimestamp();
            }

            @Override
            public Long getResultTime() {
                return null;
            }

            @Override
            public Long getVendorSettleTime() {
                return null;
            }

            @Override
            public BigDecimal getJackpotAmount() {
                return BigDecimal.ZERO;
            }

            @Override
            public Integer getIsFreespin() {
                return 0;
            }

            @Override
            public BetStatus getBetStatus() {
                return BetStatus.UNSETTLED;
            }
        };
    }

    private HttpRequestLog toHttpRequestLog(LogContext logContext) {
        final Integer PROCESSING = 1;

        HttpRequestLog httpRequestLog = new HttpRequestLog();
        httpRequestLog.setId(logContext.getTraceId());
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
