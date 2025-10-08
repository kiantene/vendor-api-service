package com.nextgen.gameaggregator.service.data;

import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.enums.TxnStatus;

import java.math.BigDecimal;
import java.time.Duration;

public interface GameTransactionDataService {
    GameTransaction findById(String id);
    void insert(GameTransaction doc);
    void insertWithTTL(GameTransaction doc, Duration ttl);
    void update(GameTransaction doc);
    void updateStatus(GameTransaction doc, BigDecimal balance, TxnStatus status);
    void updateToSettled(String docId, long settledTime);
    void updateToRefunded(String docId);
    void deleteById(String id);
}
