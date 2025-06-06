package com.nextgen.gameaggregator.core;

import com.nextgen.gameaggregator.entity.ga.RequestIdempotentLog;
import com.nextgen.gameaggregator.exception.TransactionStillProcessingException;
import com.nextgen.gameaggregator.operator.sport.adjustment.SportAdjustmentData;
import com.nextgen.gameaggregator.operator.sport.refund.SportRefundData;
import com.nextgen.gameaggregator.operator.sport.settle.SportBetResultData;
import com.nextgen.gameaggregator.operator.sport.unsettle.SportUnsettleData;
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
    String generateBetResultRequestIdempotentLogId(SportBetResultData sportBetResultData, String vendorPlayerUsername);
    String generateRefundRequestIdempotentLogId(SportRefundData sportRefundData, String vendorPlayerUsername);
    String generateUnsettleRequestIdempotentLogId(SportUnsettleData sportUnsettleData, String vendorPlayerUsername);
    String generateAdjustmentRequestIdempotentLogId(SportAdjustmentData sportAdjustmentData, String vendorPlayerUsername);

    void delete(SportBetResultData sportBetResultData, String vendorPlayerUsername);
    void delete(SportRefundData sportRefundData, String vendorPlayerUsername);
    void delete(SportUnsettleData sportUnsettleData, String vendorPlayerUsername);
    void delete(SportAdjustmentData sportAdjustmentData, String vendorPlayerUsername);
    
    RequestIdempotentLog create(SportBetResultData sportBetResultData, String vendorPlayerUsername);
    RequestIdempotentLog create(SportRefundData sportRefundData, String vendorPlayerUsername);
    RequestIdempotentLog create(SportUnsettleData sportUnsettleData, String vendorPlayerUsername);
    RequestIdempotentLog create(SportAdjustmentData sportAdjustmentData, String vendorPlayerUsername);

    RequestIdempotentLog checkExists(SportBetResultData sportBetResultData, String vendorPlayerUsername) throws TransactionStillProcessingException;
    RequestIdempotentLog checkExists(SportRefundData sportRefundData, String vendorPlayerUsername) throws TransactionStillProcessingException;
    RequestIdempotentLog checkExists(SportUnsettleData sportUnsettleData, String vendorPlayerUsername) throws TransactionStillProcessingException;
    RequestIdempotentLog checkExists(SportAdjustmentData sportAdjustmentData, String vendorPlayerUsername) throws TransactionStillProcessingException;
}
