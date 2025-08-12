package com.nextgen.gameaggregator.core;

import com.nextgen.gameaggregator.entity.ga.RequestIdempotentLog;
import com.nextgen.gameaggregator.exception.TransactionStillProcessingException;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;

public interface RequestIdempotentLogService {

    String generateRequestIdempotentLogId(BetResultData betResultData, String vendorPlayerUsername);

    String generateRollbackRequestIdempotentLogId(RollbackData rollbackData, String vendorPlayerUsername);

    void delete(BetResultData betResultData, String vendorPlayerUsername);

    void delete(RollbackData rollbackData, String vendorPlayerUsername);

    RequestIdempotentLog checkExists(BetResultData betResultData, String vendorPlayerUsername) throws TransactionStillProcessingException;

    RequestIdempotentLog checkExists(RollbackData rollbackData, String vendorPlayerUsername) throws TransactionStillProcessingException;

    RequestIdempotentLog create(BetResultData betResultData, String vendorPlayerUsername);

    RequestIdempotentLog create(RollbackData rollbackData, String vendorPlayerUsername);

    RequestIdempotentLog save(RequestIdempotentLog requestIdempotentLog);

    RequestIdempotentLog get(String id);

    // Sportsbook specific methods
    // To differentiate between different types of requests, we use a prefix which pass in from class files (Discussed with Jeff, 18 Jun 2025)
    // TODO: Consider improving this by using a more structured approach
    String generateBetResultRequestIdempotentLogId(String externalTransactionId, String vendorPlayerUsername, String prefix);

    void delete(String externalTransactionId, String vendorPlayerUsername, String prefix);

    RequestIdempotentLog create(String externalTransactionId, String vendorPlayerUsername, String prefix);

    RequestIdempotentLog getSportsRequestIdempotentLog(String externalTransactionId, String vendorPlayerUsername, String prefix);
}
