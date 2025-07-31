package com.nextgen.gameaggregator.core.engine.wallet.result;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.exception.InternalServerException;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WalletBetResultServiceWrapper implements WalletBetResultService {

    private final Map<String, BaseVendorService> vendorServiceMap = new HashMap<>();
    private final Map<String, Boolean> transactionIsBetMap = new HashMap<>();
    private final WalletService walletService;
    private final HttpService httpService;

    public WalletBetResultServiceWrapper isBetTxn(BetResultContext context, boolean flag) {
        transactionIsBetMap.put(context.getIdempotencyKey(), flag);
        return this;
    }

    public WalletBetResultServiceWrapper vendorService(BetResultContext context, BaseVendorService vendorService) {
        vendorServiceMap.put(context.getVendorClassName(), vendorService);
        return this;
    }

    @Override
    public PlayerBalanceData process(BetResultContext context) {
        context.setResultTime(System.currentTimeMillis());
        LogContext logContext = LogContextHolder.get();
        final String vendorClassName = logContext.getVendorClassName();

        BetResultData betResultData = this.toBetResultData(context);
        HttpRequestLog httpRequestLog = this.toHttpRequestLog(logContext);
        BaseVendorService vendorService = vendorServiceMap.get(vendorClassName); // assume not null
        ResultType resultType = getResultType(context);
        GameSession gameSession = new GameSession();

        try {
            BigDecimal balance = walletService.processBetResult(context.getTraceId(), gameSession, betResultData, resultType, vendorService, httpRequestLog);
            return this.toPlayerBalanceData(context, balance, httpRequestLog);

        } catch (Exception ex) {
            throw new InternalServerException(ex.getMessage(), ex);

        } finally {
            cleanup(context);
            httpService.end(httpRequestLog, null);
        }
    }

    private BetResultData toBetResultData(BetResultContext context) {
        return new BetResultData() {
            @Override
            public String getExternalTransactionId() {
                return context.getIdempotencyKey();
            }

            @Override
            public String getVendorBetId() {
                return context.getVendorBetId();
            }

            @Override
            public String getRoundId() {
                return context.getRoundId();
            }

            @Override
            public String getGameId() {
                return context.getGameCode();
            }

            @Override
            public BigDecimal getBetAmount() {
                return context.getBetAmount();
            }

            @Override
            public BigDecimal getWinAmount() {
                return context.getWinAmount();
            }

            @Override
            public BigDecimal getWinLoss() {
                return context.getWinloss();
            }

            @Override
            public BigDecimal getEffectiveTurnover() {
                return context.getEffectiveTurnover();
            }

            @Override
            public Long getVendorBetTime() {
                return context.getVendorBetTime();
            }

            @Override
            public Long getResultTime() {
                return System.currentTimeMillis();
            }

            @Override
            public Long getVendorSettleTime() {
                return context.getVendorSettleTime();
            }

            @Override
            public BigDecimal getJackpotAmount() {
                return context.getJackpotAmount();
            }

            @Override
            public Integer getIsFreespin() {
                return context.getIsFreeSpin();
            }

            @Override
            public BetStatus getBetStatus() {
                return BetStatus.SETTLED;
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

    private PlayerBalanceData toPlayerBalanceData(BetResultContext context, BigDecimal balance, HttpRequestLog httpRequestLog) {
        PlayerBalanceData playerBalanceData = new PlayerBalanceData();

        playerBalanceData.setUsername(context.getVendorPlayerUsername());
        playerBalanceData.setBalance(balance);
        playerBalanceData.setCurrency(context.getVendorCurrency());
        playerBalanceData.setTimestamp(httpRequestLog.getOperatorEnd());

        return playerBalanceData;
    }

    private ResultType getResultType(BetResultContext context) {
        String idempotencyKey = context.getIdempotencyKey();
        /*
        Scenarios:
        1. WIN        -> Win transaction for a previous bet (not a bet)
        2. BET_WIN    -> Bet with win or jackpot
        3. BET_LOSE   -> Bet with no win
        4. END        -> Non-bet transaction with no win (default fallback)
        */

        boolean isBet = transactionIsBetMap.getOrDefault(idempotencyKey, true);
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

    private void cleanup(BetResultContext context) {
        transactionIsBetMap.remove(context.getIdempotencyKey());
        vendorServiceMap.remove(context.getVendorClassName());
    }
}
