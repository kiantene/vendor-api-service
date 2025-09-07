package com.nextgen.gameaggregator.core.engine.wallet.bet;

import com.nextgen.gameaggregator.core.context.BaseEnricher;
import com.nextgen.gameaggregator.core.service.AgentPlayerDataService;
import com.nextgen.gameaggregator.core.service.VendorGameDataService;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import org.springframework.stereotype.Service;

@Service
class BetContextEnricher extends BaseEnricher<BetContext> {

    public BetContextEnricher(AgentPlayerDataService agentPlayerDataService,
                              VendorPlayerDataService vendorPlayerDataService,
                              VendorGameDataService vendorGameDataService) {
        super(agentPlayerDataService, vendorPlayerDataService, vendorGameDataService);
    }

    @Override
    protected void doEnrich(BetContext context) {
        if (context.getTimestamp() == null) {
            context.setTimestamp(System.currentTimeMillis());
        }
        if (context.getVendorBetId() == null) {
            context.setVendorBetId(context.getIdempotencyKey());
        }
    }

    public void enrichByGameSession(BetContext context, GameSession gameSession) {
        // null check is done in gameSessionDataService.getGameSession, so we won't do null check here
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
    }

    public void enrichGameTransaction(GameTransaction txn, BetContext context) {
        txn.setUsername(context.getVendorPlayerUsername());
        txn.setRoundId(context.getRoundId());
        txn.setGameCode(context.getVendorGameCode());
        txn.setCurrency(context.getVendorCurrency());
        txn.setBetAmount(context.getBetAmount());
        txn.setBetTime(context.getTimestamp());
    }
}
