package com.nextgen.gameaggregator.core.engine.wallet.result;

import com.nextgen.gameaggregator.core.context.BaseEnricher;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.service.AgentPlayerDataService;
import com.nextgen.gameaggregator.core.service.InternalVendorService;
import com.nextgen.gameaggregator.core.service.VendorGameDataService;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.enums.GameRoundState;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
class BetResultContextEnricher extends BaseEnricher<BetResultContext> {
    private final ApplicationContext applicationContext;

    public BetResultContextEnricher(AgentPlayerDataService agentPlayerDataService,
                                    VendorPlayerDataService vendorPlayerDataService,
                                    VendorGameDataService vendorGameDataService,
                                    ApplicationContext applicationContext) {
        super(agentPlayerDataService, vendorPlayerDataService, vendorGameDataService);
        this.applicationContext = applicationContext;
    }

    @Override
    protected void doEnrich(BetResultContext context) {
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

        BetResultWrapperContext wrapperContext = BetResultContextHolder.getRequired();
        if (wrapperContext.getVendorService() == null) {
            wrapperContext.setVendorService(InternalVendorService.getInstance(applicationContext));
        }
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

        if (context.getRoundEnded() == null && config.getSettleType() == SettleType.BET) {
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
        txn.setBetTime(context.getVendorBetTime());
        txn.setSettleTime(context.getVendorSettleTime());
        txn.setState(GameRoundState.SETTLED);
    }
}
