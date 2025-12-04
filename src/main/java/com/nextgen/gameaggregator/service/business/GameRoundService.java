package com.nextgen.gameaggregator.service.business;

import com.nextgen.gameaggregator.core.exception.RoundNotFoundException;
import com.nextgen.gameaggregator.entity.couchbase.*;
import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.enums.TxnStatus;
import com.nextgen.gameaggregator.service.data.GameRoundDataService;
import com.nextgen.gameaggregator.service.data.model.TxnDelta;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class GameRoundService {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter
            .ofPattern("HH:mm:ss.SSS")
            .withZone(ZoneOffset.UTC);
    private static final Duration ROUND_TTL = Duration.ofHours(6);
    private final GameRoundDataService data;

    public GameRoundService(GameRoundDataService data) {
        this.data = data;
    }

    public Optional<GameRound> get(String id) {
        var doc = data.findById(id);

        if (doc == null) return Optional.empty();

        return Optional.of(doc.getPayload());
    }

    public GameRound getOrThrow(String id) {
        return get(id).orElseThrow(() -> new RoundNotFoundException("GameRound not found: " + id));
    }

    public KvDoc<GameRound> getDoc(String id) {
        return data.findById(id);
    }

    public GameRound save(GameTransaction txn, AgentMeta agentMeta) {
        String docId = txn.getRoundDocId();
        KvDoc<GameRound> kvDoc = getDoc(docId);

        // New round document
        if (kvDoc == null) {
            GameRound newRound = buildRound(txn, agentMeta);
            data.insert(newRound);
            return newRound;
        }

        return addNewTxnToRound(kvDoc, txn);
    }

    public GameRound applyTxnDelta(TxnDelta delta) {
        return data.applyTxnDelta(delta, ROUND_TTL);
    }

    public void updateRoundState(String docId, GameRoundState state) {
        data.setRoundState(docId, state);
    }

    public Mono<Void> markTxnErrorAsync(GameTransaction txn, RuntimeException ex) {
        return Mono.fromRunnable(() -> markTxnError(txn, ex))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    public void markTxnError(GameTransaction txn, RuntimeException ex) {
        if (txn == null) return;

        String exName = getMeaningfulExceptionName(ex);
        Map<String, Object> updates = Map.of(
                "status", TxnStatus.ERROR.name(),
                "exception", exName,
                "doneAt", getNow()
        );

        try {
            // if idx is null, means that an exception is thrown before txn is being saved
            if (txn.getIdx() == null) {
                txn.setStatus(TxnStatus.ERROR);
                txn.setException(exName);
                txn.setDoneAt(getNow());
                save(txn, null);
            } else {
                data.updateTxn(txn.getRoundDocId(), txn.getIdx(), updates);
            }
        } catch (Exception e) {
            log.error("markTxnError failed on: " + e.getMessage());
        }
    }

    public void updateRoundTxn(String docId, int idx, Map<String, Object> updates) {
        data.updateTxn(docId, idx, updates);
    }

    public void updateRoundTxn(GameTransaction txn, GameRoundState state) {
        data.updateTxn(txn.getRoundDocId(), txn.getIdx(), Map.of("state", state));
    }

    private String getMeaningfulExceptionName(RuntimeException ex) {
        Throwable current = ex;

        // Keep unwrapping while we have generic RuntimeException with causes
        while (current.getClass() == RuntimeException.class && current.getCause() != null) {
            current = current.getCause();
        }

        return current.getClass().getSimpleName();
    }

    private GameRound buildRound(GameTransaction txn, AgentMeta agentMeta) {
        GameRound round = GameRound.of(txn.getClassName(), txn.getUsername(), txn.getRoundId());
        round.setVendorId(txn.getVendorId());
        round.setUsername(txn.getUsername());
        round.setGameCode(txn.getGameCode());
        round.setCurrency(txn.getCurrency());
        round.setAgentMeta(agentMeta);
        round.setTransactions(List.of(RoundTxn.of(txn)));
        round.setCreatedAt(txn.getCreatedAt());
        round.setCreatedTs(txn.getCreatedTs());
        txn.setIdx(0);
    
        // If the transaction is marked as VOID, set the round state to VOID
        if (txn.getState() == GameRoundState.VOID) {
            round.setState(GameRoundState.VOID);
        }

        return round;
    }

    private GameRound addNewTxnToRound(KvDoc<GameRound> kvDoc, GameTransaction txn) {
        // Existing round document
        GameRound round = kvDoc.getPayload();

        // Idempotency: skip if this transaction already exists in the round
        if (isTransactionExists(round, txn)) {
            return round; // no overwrite / no double-count
        }

        RoundTxn roundTxn = RoundTxn.of(txn);

        // Persist append atomically with CAS
        data.appendTxn(round.getId(), roundTxn, kvDoc.getCas());

        int txnCount = (round.getTxnCount() == null ? 0 : round.getTxnCount());
        // Reflect in-memory changes on txn idx for further updates later
        txn.setIdx(txnCount);
        round.setTxnCount(txnCount + 1);

        if (round.getTransactions() != null) {
            round.getTransactions().add(roundTxn);
        }

        return round;
    }

    private boolean isTransactionExists(GameRound round, GameTransaction txn) {
        List<RoundTxn> txnList = round.getTransactions();

        if (txnList == null || txnList.isEmpty()) {
            return false;
        }

        String transactionIdToFind = txn.getId();

        // Check if any existing transaction has the same ID
        return txnList.stream().anyMatch(t -> transactionIdToFind.equals(t.getId()));
    }

    private String getNow() {
        return TIME_FORMATTER.format(Instant.now());
    }
}
