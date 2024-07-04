package com.nextgen.gameaggregator.sport.service;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.sport.adjustment.SportAdjustmentData;
import com.nextgen.gameaggregator.operator.sport.refund.SportRefundData;
import com.nextgen.gameaggregator.operator.sport.resettle.SportResettleData;
import com.nextgen.gameaggregator.operator.sport.settle.SportBetResultData;
import com.nextgen.gameaggregator.operator.sport.unsettle.SportUnsettleData;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public interface SportWalletService {
    ExecutorService THREAD_POOL = Executors.newFixedThreadPool(32);

    WalletRequest placeBet(WalletRequest walletRequest)
            throws InsufficientBalanceException, InvalidOperatorResponseException, BetResultIdempotentViolationException, TransactionStillProcessingException;

    WalletRequest placeMultipleBets(WalletRequest walletRequest)
            throws InsufficientBalanceException, InvalidOperatorResponseException, BetResultIdempotentViolationException, TransactionStillProcessingException;

    WalletRequest confirmBet(WalletRequest walletRequest) throws
            BetNotFoundException, BetNotAllowedException, BetResultIdempotentViolationException,
            InvalidOperatorResponseException, TransactionStillProcessingException;

    BetEvent settle(String traceId, SportBetResultData sportBetResultData, HttpRequestLog httpRequestLog)
            throws BetNotFoundException, InvalidAgentApiCredentialException, RecordNotFoundException, InvalidOperatorResponseException, BetResultIdempotentViolationException;

    WalletRequest refund(WalletRequest walletRequest) throws
            BetNotFoundException, BetNotAllowedException, BetResultIdempotentViolationException,
            InvalidOperatorResponseException, TransactionStillProcessingException;

    BetEvent refund(String traceId, SportRefundData sportRefundData, HttpRequestLog httpRequestLog)
            throws VendorCurrencyNotSupportException, InsufficientBalanceException, InvalidOperatorResponseException, InvalidAgentApiCredentialException, BetNotFoundException, TransactionStillProcessingException, BetResultIdempotentViolationException, RecordNotFoundException;

    BetEvent unsettle(String traceId, SportUnsettleData sportUnsettleData, String rawData, HttpRequestLog httpRequestLog)
            throws VendorCurrencyNotSupportException, InsufficientBalanceException, InvalidOperatorResponseException, InvalidAgentApiCredentialException, BetNotFoundException, InvalidPlayerException, BetResultIdempotentViolationException;

    BetEvent resettle(String traceId, SportResettleData sportResettleData, HttpRequestLog httpRequestLog)
            throws InvalidOperatorResponseException, BetNotFoundException, BetResultIdempotentViolationException;

    BetEvent adjustment(String traceId, SportAdjustmentData sportAdjustmentData, HttpRequestLog httpRequestLog)
            throws InvalidOperatorResponseException, BetNotFoundException, TransactionStillProcessingException, BetAdjustmentIdempotentViolationException, InvalidPlayerException, RecordNotFoundException, VendorCurrencyNotSupportException, InsufficientBalanceException;

}
