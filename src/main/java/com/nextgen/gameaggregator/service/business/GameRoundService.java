package com.nextgen.gameaggregator.service.business;

import com.nextgen.gameaggregator.entity.couchbase.*;
import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.enums.TxnStatus;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.data.GameRoundDataService;
import com.nextgen.gameaggregator.service.data.model.TxnDelta;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
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

    public boolean isResultBeforeBet(GameRound round, ResultType resultType) {
        boolean isBetType = ResultType.BET_WIN == resultType || ResultType.BET_LOSE == resultType;
        return round.getTxnCount() == 1 // only 1 transaction in game_round
                && !isBetType; // not a bet type
    }

    public void updateRoundState(String docId, GameRoundState state) {
        data.setRoundState(docId, state);
    }

    public void markTxnError(GameTransaction txn, RuntimeException ex) {
        if (txn == null) return;

        String exName = ex.getClass().getSimpleName();
        if (exName.equals("RuntimeException") && ex.getCause() != null) {
            exName = ex.getCause().getClass().getSimpleName();
        }

        Map<String, Object> updates = Map.of(
                "status", TxnStatus.ERROR.name(),
                "exception", exName,
                "doneAt", getNow()
        );

        data.updateTxn(txn.getRoundDocId(), txn.getIdx(), updates);
    }

    private GameRound buildRound(GameTransaction txn, AgentMeta agentMeta) {
        GameRound round = GameRound.of(txn.getClassName(), txn.getRoundId());
        round.setVendorId(txn.getVendorId());
        round.setUsername(txn.getUsername());
        round.setGameCode(txn.getGameCode());
        round.setCurrency(txn.getCurrency());
        round.setAgentMeta(agentMeta);
        round.setTransactions(List.of(RoundTxn.of(txn)));
        round.setCreatedAt(txn.getCreatedAt());
        round.setCreatedTs(txn.getCreatedTs());
        txn.setIdx(0);

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
