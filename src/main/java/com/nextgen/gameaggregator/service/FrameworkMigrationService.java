package com.nextgen.gameaggregator.service;

import java.util.Optional;

import com.nextgen.gameaggregator.core.vendor.config.VendorConfigService;
import org.springframework.stereotype.Service;

import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultConfig;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.idempotency.DuplicateRequestGuard;
import com.nextgen.gameaggregator.core.service.GameSessionDataService;
import com.nextgen.gameaggregator.entity.couchbase.AgentMeta;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.UnsettledBet;
import com.nextgen.gameaggregator.enums.TxnType;
import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.service.business.GameRoundService;
import com.nextgen.gameaggregator.service.business.GameTransactionService;
//import com.nextgen.gameaggregator.vendor.Vendors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FrameworkMigrationService {
    private final GameTransactionService gameTransactionService;
    private final UnsettledBetService unsettledBetService;
    private final GameRoundService gameRoundService;
    private final GameSessionDataService gameSessionDataService;
    private final VendorConfigService vendorConfigService;
    private final DuplicateRequestGuard guard;
     
    public boolean isFallbackRequired(Optional<GameRound> roundOpt, String className, BetResultConfig config) {
        return roundOpt.isEmpty() 
            && isMigrationVendor(className) 
            && config.isSettledByBet()
            && !config.isBetAndResult();
    }
    
    public boolean isMigrationVendor(String className) {
        return vendorConfigService.isMigrationVendor(className);
    }

    public Optional<GameRound> createBetTransaction(
            BetResultContext context, String className) throws BetNotFoundException {

        GameSession gameSession = gameSessionDataService.getOrCreate(context);

        UnsettledBet unsettledBet = unsettledBetService.getUnsettledBetByRoundId(
                context.getVendorBetId(),
                context.getRoundId(),
                gameSession.getVendorGameId(),
                gameSession.getVendorPlayerId()
        );

        GameTransaction txn = guard.ensureNotDuplicate(
                TxnType.BET,
                className,
                context.getIdempotencyKey(),
                unsettledBet.getCreateTime()
        );

        enrichGameTransaction(txn, context, unsettledBet, gameSession);
        GameRound round = gameTransactionService.markSent(txn, buildAgentMeta(gameSession));

        gameTransactionService.markSuccess(round, txn, unsettledBet.getBalance());

        return gameRoundService.get(round.getId());
    }

    private void enrichGameTransaction(GameTransaction txn, BetResultContext context, UnsettledBet unsettledBet, GameSession gameSession) {
        txn.setVendorBetId(unsettledBet.getVendorBetId());
        txn.setVendorId(unsettledBet.getVendorId());
        txn.setUsername(context.getVendorPlayerUsername());
        txn.setRoundId(unsettledBet.getRoundId());
        txn.setGameCode(gameSession.getVendorGameCode());
        txn.setCurrency(gameSession.getVendorCurrencyCode());
        txn.setBetAmount(unsettledBet.getBetAmount());
        txn.setBetTime(unsettledBet.getVendorBetTime());
    }

    private AgentMeta buildAgentMeta(GameSession gameSession) {
        AgentMeta agentMeta = new AgentMeta();
        agentMeta.setAgentId(gameSession.getAgentId());
        agentMeta.setUsername(gameSession.getAgentPlayerUsername());
        agentMeta.setCurrency(gameSession.getCurrencyCode());
        agentMeta.setGameCode(gameSession.getGameCode());
        agentMeta.setSession(gameSession.getToken());

        return agentMeta;
    }
}
