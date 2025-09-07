package com.nextgen.gameaggregator.core.engine.wallet.result;

import com.nextgen.gameaggregator.core.context.BaseEnricher;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.service.AgentPlayerDataService;
import com.nextgen.gameaggregator.core.service.InternalVendorService;
import com.nextgen.gameaggregator.core.service.VendorGameDataService;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
class BetResultContextEnricher extends BaseEnricher<BetResultContext> {
    private final ApplicationContext applicationContext;

    protected BetResultContextEnricher(AgentPlayerDataService agentPlayerDataService,
                                       VendorPlayerDataService vendorPlayerDataService,
                                       VendorGameDataService vendorGameDataService,
                                       ApplicationContext applicationContext) {
        super(agentPlayerDataService, vendorPlayerDataService, vendorGameDataService);
        this.applicationContext = applicationContext;
    }

    @Override
    protected void doEnrich(BetResultContext context) {
        context.setResultTime(System.currentTimeMillis());

        if (context.getVendorBetId() == null) {
            context.setVendorBetId(context.getIdempotencyKey());
        }

        if (context.getBetAmount() == null) {
            context.setBetAmount(BigDecimal.ZERO);
        }

        if (context.getIsFreeSpin() == null) {
            context.setIsFreeSpin(0);
        }

        BetResultWrapperContext wrapperContext = BetResultContextHolder.getRequired();
        if (wrapperContext.getVendorService() == null) {
            wrapperContext.setVendorService(InternalVendorService.getInstance(applicationContext));
        }
    }

    public void enrichByGameSession(BetResultContext context, GameSession gameSession) {
        if (context.getVendorPlayerUsername() == null) {
            context.setVendorPlayerUsername(gameSession.getVendorPlayerUsername());
        }
        if (context.getVendorCurrency() == null) {
            context.setVendorCurrency(gameSession.getVendorCurrencyCode());
        }
        if (context.getCurrencyCode() == null) {
            context.setCurrencyCode(gameSession.getCurrencyCode());
        }
    }

    public void enrichGameTransaction(GameTransaction txn, BetResultContext context) {
        txn.setUsername(context.getVendorPlayerUsername());
        txn.setRoundId(context.getRoundId());
        txn.setGameCode(context.getVendorGameCode());
        txn.setCurrency(context.getVendorCurrency());
        txn.setBetAmount(context.getBetAmount());
        txn.setWinAmount(context.getWinAmount());
        txn.setBetTime(context.getVendorBetTime());
        txn.setSettleTime(context.getVendorSettleTime());
    }
}
