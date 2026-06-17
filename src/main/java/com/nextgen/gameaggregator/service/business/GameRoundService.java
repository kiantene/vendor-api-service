package com.nextgen.gameaggregator.service.business;

import com.couchbase.client.core.error.DocumentExistsException;
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
            Optional<KvDoc<GameRound>> existingRound = data.insertOrGet(newRound);
            if (existingRound.isPresent()) {
                // Lost the insert race; fall through and append to the round
                // someone else just inserted. txn.idx remains null until appendTxn
                // reports the server-authoritative slot.
                kvDoc = existingRound.get();
            } else {
                // Won the insert race; the new round has this txn's RoundTxn at index 0.
                txn.setIdx(0);
                return newRound;
            }
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
        // NOTE: txn.idx is intentionally NOT set here. It is the caller's
        // responsibility (save()) to set idx=0 only after the insert race is
        // won, so threads that lose the race and fall through to addNewTxnToRound
        // do not carry a stale idx into a CAS failure path.

        // If the transaction is marked as VOID, set the round state to VOID
        if (txn.getState() == GameRoundState.VOID) {
            round.setState(GameRoundState.VOID);
        }

        return round;
    }

    private GameRound addNewTxnToRound(KvDoc<GameRound> kvDoc, GameTransaction txn) {
        // Existing round document
        GameRound round = kvDoc.getPayload();

        // Idempotency: if this txn was already appended (e.g. by a prior attempt
        // that failed later and left its RoundTxn behind), do NOT append again —
        // but DO restore idx from the existing slot so later updates
        // (markSuccess/markSettled) target the correct transactions[idx] instead
        // of carrying a null idx into markSuccess and unboxing it.
        int existingIdx = indexOfTxn(round, txn);
        if (existingIdx >= 0) {
            txn.setIdx(existingIdx);
            return round; // no overwrite / no double-count
        }

        RoundTxn roundTxn = RoundTxn.of(txn);

        // appendTxn returns the server-authoritative index of the just-appended
        // RoundTxn. The append is concurrency-safe without CAS — commutative
        // arrayAppend + atomic counter increment — so concurrent siblings each
        // get a distinct, correct idx.
        int newIdx = data.appendTxn(round.getId(), roundTxn);

        txn.setIdx(newIdx);
        round.setTxnCount(newIdx + 1);

        if (round.getTransactions() != null) {
            round.getTransactions().add(roundTxn);
        }

        return round;
    }

    private int indexOfTxn(GameRound round, GameTransaction txn) {
        List<RoundTxn> txnList = round.getTransactions();

        if (txnList == null || txnList.isEmpty()) {
            return -1;
        }

        String transactionIdToFind = txn.getId();

        // Array position is the authoritative idx: RoundTxns are only ever appended
        // (never removed or reordered), so list index == transactions[idx].
        for (int i = 0; i < txnList.size(); i++) {
            if (transactionIdToFind.equals(txnList.get(i).getId())) {
                return i;
            }
        }
        return -1;
    }

    private String getNow() {
        return TIME_FORMATTER.format(Instant.now());
    }
}
