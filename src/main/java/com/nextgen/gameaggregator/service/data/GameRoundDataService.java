package com.nextgen.gameaggregator.service.data;

import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.KvDoc;
import com.nextgen.gameaggregator.entity.couchbase.RoundTxn;
import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.service.data.model.TxnDelta;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

public interface GameRoundDataService {
    KvDoc<GameRound> findById(String id);
    void insert(GameRound round);

    /**
     * Return Empty if Insert is Successful
     * Return Existing GameRound if Insert failed due to existing record
     */
    Optional<KvDoc<GameRound>> insertOrGet(GameRound round);
    void appendTxn(String docId, RoundTxn roundTxn, long cas);
    void updateTxn(String docId, int idx, Map<String, Object> updates);
    void setRoundState(String docId, GameRoundState state);
    GameRound applyTxnDelta(TxnDelta delta, Duration ttl);
}
