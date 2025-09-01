package com.nextgen.gameaggregator.service.data;

import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.enums.TxnStatus;

import java.math.BigDecimal;

public interface GameTransactionDataService {
    GameTransaction findById(String id);
    void insert(GameTransaction doc);
    void updateStatus(GameTransaction doc, BigDecimal balance, TxnStatus status);
    void deleteById(String id);
}
