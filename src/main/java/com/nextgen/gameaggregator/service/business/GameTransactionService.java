package com.nextgen.gameaggregator.service.business;

import com.nextgen.gameaggregator.entity.couchbase.AgentMeta;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.entity.couchbase.RoundTxn;
import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.enums.TxnStatus;
import com.nextgen.gameaggregator.service.data.GameTransactionDataService;
import com.nextgen.gameaggregator.service.data.model.TxnDelta;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GameTransactionService {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter
            .ofPattern("HH:mm:ss.SSS")
            .withZone(ZoneOffset.UTC);

    private final GameTransactionDataService txnDataService;
    private final GameRoundService gameRoundService;

    public Optional<GameTransaction> get(GameTransaction txn) {
        var doc = txnDataService.findById(txn.getId());

        if (doc == null) return Optional.empty();

        return Optional.of(doc);
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

        return gameRoundService.save(txn, agentMeta);
    }

    public void markPending(GameTransaction txn) {
        // TODO: update status
    }

    public void markSuccess(GameRound round, GameTransaction txn, BigDecimal balance) {
        markSuccess(round, txn, balance, false);
    }

    public void markSuccess(GameRound round, GameTransaction txn, BigDecimal balance, Boolean isEnded) {
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
                txn.getDoneAt(),
                GameRoundState.SETTLED == txn.getState(),
                Optional.ofNullable(isEnded).orElse(false)
        );

        gameRoundService.applyTxnDelta(delta);

        if (Boolean.TRUE.equals(isEnded)) {
            round.getTransactions()
                    .stream()
                    .filter(RoundTxn::isSuccessfulBet)
                    .forEach(t -> markSettled(t.getId(), txn.getSettleTime()));
        }
    }

    public void markSettled(String txnId, long settledTime) {
        Duration ttl = Duration.ofHours(3);
        txnDataService.updateToSettled(txnId, settledTime, ttl);
    }

    private String getNow() {
        return TIME_FORMATTER.format(Instant.now());
    }

    public void deleteById(String id) {
        txnDataService.deleteById(id);
    }
}
