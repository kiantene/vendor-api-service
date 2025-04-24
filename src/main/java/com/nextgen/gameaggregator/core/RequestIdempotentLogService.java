package com.nextgen.gameaggregator.core;

import com.nextgen.gameaggregator.entity.ga.RequestIdempotentLog;
import com.nextgen.gameaggregator.exception.TransactionStillProcessingException;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;

public interface RequestIdempotentLogService {

    String generateRequestIdempotentLogId(BetResultData betResultData, String vendorPlayerUsername);
    String generateRollbackRequestIdempotentLogId(RollbackData rollbackData, String vendorPlayerUsername);
    void delete(BetResultData betResultData, String vendorPlayerUsername);
    void deleteRollback(RollbackData rollbackData, String vendorPlayerUsername);
    RequestIdempotentLog checkExists(BetResultData betResultData, String vendorPlayerUsername) throws TransactionStillProcessingException;
    RequestIdempotentLog checkExistsRollback(RollbackData rollbackData, String vendorPlayerUsername) throws TransactionStillProcessingException;
    RequestIdempotentLog create(BetResultData betResultData, String vendorPlayerUsername);
    RequestIdempotentLog createRollback(RollbackData rollbackData, String vendorPlayerUsername);
    RequestIdempotentLog save(RequestIdempotentLog requestIdempotentLog);
    RequestIdempotentLog get(String id);
}
