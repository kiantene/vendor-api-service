package com.nextgen.gameaggregator.core.engine.wallet.result;

import com.nextgen.core.exception.InternalServerException;
import com.nextgen.gameaggregator.core.context.BaseEnricher;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.logging.LogContextService;
import com.nextgen.gameaggregator.core.service.*;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.exception.GameNotSupportedException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Service
class BetResultContextEnricher extends BaseEnricher<BetResultContext> {
    private final ApplicationContext applicationContext;

    public BetResultContextEnricher(AgentPlayerDataService agentPlayerDataService,
                                    VendorPlayerDataService vendorPlayerDataService,
                                    VendorGameDataService vendorGameDataService,
                                    CurrencyDataService currencyDataService,
                                    VendorCurrencyDataService vendorCurrencyDataService,
                                    ApplicationContext applicationContext) {
        super(agentPlayerDataService, vendorPlayerDataService, vendorGameDataService, currencyDataService, vendorCurrencyDataService);
        this.applicationContext = applicationContext;
    }

    @Override
    protected void doEnrich(BetResultContext context) {
        setDefaultIfEmpty(context);

        BetResultWrapperContext wrapperContext = BetResultContextHolder.getRequired();
        if (wrapperContext.getVendorService() == null) {
            wrapperContext.setVendorService(InternalVendorService.getInstance(applicationContext));
            try {
                wrapperContext.getVendorService().verifyIsPreProcessingVendorGame(context.getVendorGameId());
            } catch (GameNotSupportedException e) {
                throw new InternalServerException(e.getMessage(), e);
            }
        }

        // populateLogContext must be run in doEnrich so that context object will contain all required fields
        LogContext logContext = LogContextHolder.get();
        LogContextService.populateLogContext(logContext, context);

        context.setTraceId(logContext.getTraceId());
    }

    public void enrichByGameSession(BetResultContext context, GameSession gameSession, BetResultConfig config) {
        if (context.getVendorPlayerUsername() == null) {
            context.setVendorPlayerUsername(gameSession.getVendorPlayerUsername());
        }
        if (context.getVendorCurrency() == null) {
            context.setVendorCurrency(gameSession.getVendorCurrencyCode());
        }
        if (context.getVendorGameCode() == null) {
            context.setVendorGameCode(gameSession.getVendorGameCode());
        }
        if (context.getCurrencyCode() == null) {
            context.setCurrencyCode(gameSession.getCurrencyCode());
        }
        
        enrich(context);

        if (config.isSettledByBet() && context.getRoundEnded() == null) {
            context.setRoundEnded(true);
        }
    }

    public void enrichGameTransaction(GameTransaction txn, BetResultContext context) {
        txn.setVendorBetId(context.getVendorBetId());
        txn.setVendorId(context.getVendorId());
        txn.setUsername(context.getVendorPlayerUsername());
        txn.setRoundId(context.getRoundId());
        txn.setGameCode(context.getVendorGameCode());
        txn.setCurrency(context.getVendorCurrency());
        txn.setBetAmount(context.getBetAmount());
        txn.setWinAmount(context.getWinAmount());
        txn.setJackpotAmount(context.getJackpotAmount());
        txn.setBetTime(context.getVendorBetTime());
        txn.setSettleTime(context.getVendorSettleTime());
        txn.setState(GameRoundState.SETTLED);
    }

    public Mono<Void> enrichGameTransactionIfEmpty(GameTransaction txn, BetResultContext context) {
        return Mono.defer(() -> {
            // if idx is null, means that an exception is thrown before enrichGameTransaction is called
            if (txn.getIdx() == null) {
                return Mono.fromRunnable(() -> enrichGameTransaction(txn, context));
            }
            return Mono.empty();
        });
    }

    private void setDefaultIfEmpty(BetResultContext context) {
        context.setResultTime(LogContextHolder.get().getStart());

        if (context.getVendorBetId() == null) {
            context.setVendorBetId(context.getIdempotencyKey());
        }

        if (context.getBetAmount() == null) {
            context.setBetAmount(BigDecimal.ZERO);
        }

        if (context.getIsFreeSpin() == null) {
            context.setIsFreeSpin(0);
        }

        if (context.getVendorSettleTime() == null) {
            context.setVendorSettleTime(context.getResultTime());
        }
    }
}
