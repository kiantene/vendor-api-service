package com.nextgen.gameaggregator.service.data;

import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.KvDoc;
import com.nextgen.gameaggregator.entity.couchbase.RoundTxn;
import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.service.data.model.TxnDelta;

import java.time.Duration;

public interface GameRoundDataService {
    KvDoc<GameRound> findById(String id);
    void insert(GameRound round);
    void appendTxn(String docId, RoundTxn roundTxn, long cas);
    void setRoundState(String docId, GameRoundState state);
    void applyTxnDelta(TxnDelta delta, Duration ttl);
}
