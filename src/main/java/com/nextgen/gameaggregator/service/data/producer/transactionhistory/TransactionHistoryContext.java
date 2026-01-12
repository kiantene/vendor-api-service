package com.nextgen.gameaggregator.service.data.producer.transactionhistory;

public interface TransactionHistoryContext {

    // Vendor
    String externalTransactionId();
    String vendorBetId();
    String roundId();
    Integer vendorGameId();
    Long vendorPlayerId();
    String vendorPlayerUsername();
    Integer vendorId();
    Integer vendorLineId();

    // Agent
    Integer agentId();
    Long agentPlayerId();
    String agentPlayerUsername();

    // GA
    Integer gameCategoryId();
    String gameCode();
    Integer currencyId();
    Long timestamp();

}
