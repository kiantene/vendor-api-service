package com.nextgen.gameaggregator.service.data;

import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.KvDoc;
import com.nextgen.gameaggregator.entity.couchbase.RoundTxn;
import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.enums.TxnStatus;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

public interface GameRoundDataService {
    KvDoc<GameRound> findById(String id);
    void insert(GameRound round);
    void appendTxn(String docId, RoundTxn roundTxn, BigDecimal newBetAmount, long cas);
    void setTxnStatus(String docId, int txnIndex, TxnStatus status, boolean settle, Duration ttlIfSettled);
    void setRoundState(String docId, GameRoundState state);
}
