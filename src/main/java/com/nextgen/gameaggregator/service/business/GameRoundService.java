package com.nextgen.gameaggregator.service.business;

import com.nextgen.gameaggregator.entity.couchbase.*;
import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.service.data.GameRoundDataService;
import com.nextgen.gameaggregator.service.data.model.TxnDelta;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
public class GameRoundService {
    private static final Duration ROUND_TTL = Duration.ofHours(12);
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

    public void applyTxnDelta(TxnDelta delta) {
        data.applyTxnDelta(delta, ROUND_TTL);
    }

    public void updateRoundState(String docId, GameRoundState state) {
        data.setRoundState(docId, state);
    }

    private GameRound buildRound(GameTransaction txn, AgentMeta agentMeta) {
        GameRound round = GameRound.of(txn.getVendorId(), txn.getRoundId());
        round.setUsername(txn.getUsername());
        round.setGameCode(txn.getGameCode());
        round.setCurrency(txn.getCurrency());
        round.setAgentMeta(agentMeta);
        round.setTransactions(List.of(RoundTxn.of(txn)));
        round.setCreatedAt(txn.getCreatedAt());
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
}
