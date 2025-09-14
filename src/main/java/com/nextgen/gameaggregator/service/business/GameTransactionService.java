package com.nextgen.gameaggregator.service.business;

import com.couchbase.client.core.error.DocumentExistsException;
import com.nextgen.gameaggregator.entity.couchbase.AgentMeta;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.entity.couchbase.RoundTxn;
import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.enums.TxnStatus;
import com.nextgen.gameaggregator.enums.TxnType;
import com.nextgen.gameaggregator.service.data.GameTransactionDataService;
import com.nextgen.gameaggregator.service.data.model.TxnDelta;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameTransactionService {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter
            .ofPattern("HH:mm:ss.SSS")
            .withZone(ZoneOffset.UTC);

    private final GameTransactionDataService txnDataService;
    private final GameRoundService gameRoundService;

    public Optional<GameTransaction> get(GameTransaction txn) {
        GameTransaction doc = txnDataService.findById(txn.getId());

        return doc != null ? Optional.of(doc) : Optional.empty();
    }

    public Optional<GameTransaction> get(String id) {
        GameTransaction doc = txnDataService.findById(id);

        return doc != null ? Optional.of(doc) : Optional.empty();
    }

    public GameTransaction save(GameTransaction txn) {
        if (TxnStatus.SENT == txn.getStatus()) {
            txn.setSentAt(getNow());
        }
        txnDataService.insert(txn);
        return txn;
    }

    public GameRound markSent(GameTransaction txn, AgentMeta agentMeta) {
        txn.setStatus(TxnStatus.SENT);
        txn.setSentAt(getNow());
        txnDataService.update(txn);

        /**
         * Create an alias copy of the same txn but using vendorBetId as primary key (docId)
         * This alias copy is used for rollback using vendorBetId and will expire in 7 days
         */
        if (txn.getType() == TxnType.BET && !txn.getTransactionId().equals(txn.getVendorBetId())) {
            GameTransaction betTxn = txn.copy();
            betTxn.setTransactionId(txn.getVendorBetId());
            try {
                txnDataService.insertWithTTL(txn, Duration.ofDays(7));
            } catch (DocumentExistsException ex) {
                // don't throw error even if document already exists
                log.warn("GameTransaction (" + betTxn.getId() + ") already exists");
            }
        }

        return gameRoundService.save(txn, agentMeta);
    }

    public void markPending(GameTransaction txn) {
        // TODO: update status
    }

    public void markSuccess(GameRound round, GameTransaction txn, BigDecimal balance) {
        markSuccess(round, txn, balance, false);
    }

    public GameRound markSuccess(GameRound round, GameTransaction txn, BigDecimal balance, Boolean isEnded) {
        txn.setStatus(TxnStatus.SUCCESS);
        txn.setDoneAt(getNow());
        txnDataService.updateStatus(txn, balance, TxnStatus.SUCCESS);

        TxnDelta delta = TxnDelta.finalizeSuccess(
                txn.getRoundDocId(),
                txn.getIdx(),
                txn.getGaBetId(),
                balance,
                txn.getBetAmount(),
                txn.getWinAmount(),
                txn.getJackpotAmount(),
                txn.getDoneAt(),
                GameRoundState.SETTLED == txn.getState(),
                Optional.ofNullable(isEnded).orElse(false)
        );

        GameRound updatedRound = gameRoundService.applyTxnDelta(delta);

        if (Boolean.TRUE.equals(isEnded)) {
            updatedRound.getTransactions()
                    .stream()
                    .filter(RoundTxn::isSuccessfulBet)
                    .forEach(t -> markSettled(t.getId(), txn.getSettleTime()));
        }

        return updatedRound;
    }

    public void markRollback(GameRound round, GameTransaction rollbackTxn, BigDecimal balance) {
        rollbackTxn.setState(GameRoundState.SETTLED);
        txnDataService.updateStatus(rollbackTxn, balance, TxnStatus.SUCCESS);

        gameRoundService.updateRoundState(round.getId(), GameRoundState.REFUNDED);
    }

    public void markRefunded(String txnDocId) {
        txnDataService.updateToRefunded(txnDocId);
    }

    public void markSettled(String txnId, long settledTime) {
        txnDataService.updateToSettled(txnId, settledTime);
    }

    private String getNow() {
        return TIME_FORMATTER.format(Instant.now());
    }

    public void deleteById(String id) {
        txnDataService.deleteById(id);
    }
}
