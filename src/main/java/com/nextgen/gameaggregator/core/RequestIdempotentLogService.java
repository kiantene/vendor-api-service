package com.nextgen.gameaggregator.core;

import com.nextgen.gameaggregator.entity.ga.RequestIdempotentLog;
import com.nextgen.gameaggregator.exception.TransactionStillProcessingException;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;

public interface RequestIdempotentLogService {

    String generateRequestIdempotentLogId(BetResultData betResultData, String vendorPlayerUsername);

    void delete(BetResultData betResultData, String vendorPlayerUsername);

    RequestIdempotentLog checkExists(BetResultData betResultData, String vendorPlayerUsername) throws TransactionStillProcessingException;

    RequestIdempotentLog create(BetResultData betResultData, String vendorPlayerUsername);

    RequestIdempotentLog save(RequestIdempotentLog requestIdempotentLog);
    RequestIdempotentLog get(String id);
}
