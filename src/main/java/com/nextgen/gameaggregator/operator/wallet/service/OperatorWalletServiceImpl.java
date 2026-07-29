package com.nextgen.gameaggregator.operator.wallet.service;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestServiceImpl;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.wallet.betcredit.WalletBetCreditProcessor;
import com.nextgen.gameaggregator.operator.wallet.betdebit.WalletBetDebitProcessor;
import com.nextgen.gameaggregator.operator.wallet.rollback.WalletBetDebitRefundProcessor;
import com.nextgen.gameaggregator.service.*;
import jodd.util.StringUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class OperatorWalletServiceImpl implements OperatorWalletService {

    private final WalletBetCreditProcessor walletBetCreditProcessor;
    private final WalletBetDebitProcessor walletBetDebitProcessor;
    private final WalletBetDebitRefundProcessor walletBetDebitRefundProcessor;
    private final BetResultRetryLogService betResultRetryLogService;

    public OperatorWalletServiceImpl(WalletTransactionServiceImpl walletTransactionService,
                                     WalletRequestServiceImpl walletRequestService,
                                     KafkaService kafkaService,
                                     BetResultRetryLogService betResultRetryLogService,
                                     SettledBetService settledBetService,
                                     BetResultLogService betResultLogService,
                                     BetRefundLogService betRefundLogService) {

        this.betResultRetryLogService = betResultRetryLogService;
        this.walletBetCreditProcessor = new WalletBetCreditProcessor(walletRequestService, walletTransactionService, kafkaService, settledBetService, betResultLogService, betRefundLogService);
        this.walletBetDebitProcessor = new WalletBetDebitProcessor(walletRequestService, walletTransactionService);
        this.walletBetDebitRefundProcessor = new WalletBetDebitRefundProcessor(walletRequestService, walletTransactionService, kafkaService);
    }

    @Override
    public WalletRequest betDebit(WalletRequest walletRequest) throws InvalidOperatorResponseException, InsufficientBalanceException, InternalServerException, BetNotAllowedException, BetResultIdempotentViolationException {

        try {
            walletRequest = walletBetDebitProcessor.process(walletRequest);

        } catch (BetNotAllowedException | InternalServerException e) {
            //before callToOperator
            //remain operatorStatus and balance as 0.
            throw e;

        } catch (InsufficientBalanceException e) {
            //within callToOperator and after operator response
            walletRequest.setOperatorResponseStatus(ResponseCodes.Status.SC_INSUFFICIENT_FUNDS);
            throw e;

        } catch (InvalidOperatorResponseException e) {
            //within callToOperator and after operator response
            walletRequest.setOperatorResponseStatus(ResponseCodes.Status.checkCodeStatus(e.getOperatorStatus()));
            throw e;

        } catch (Exception e) {
            //could be anywhere, so set default operatorStatus and balanceAfter if walletRequest have no value for them
            throw e;

        } finally {
            //will revisit for the walletTransaction update
            //walletTransactionService.updateOperatorStatus(walletRequest.getTraceId(), walletRequest);
            walletRequest.setBetEnd(System.currentTimeMillis());
        }

        return walletRequest;
    }

    @Override
    public WalletRequest betCredit(WalletRequest walletRequest) throws InsufficientBalanceException, InternalServerException, BetNotAllowedException, BetResultIdempotentViolationException {

        try {
            walletRequest = walletBetCreditProcessor.process(walletRequest);

        } catch (InvalidOperatorResponseException e) {
            walletRequest.setOperatorResponseStatus(ResponseCodes.Status.checkCodeStatus(e.getOperatorStatus()));
            this.setForceSuccessErrorMessageAndLog(walletRequest, e);

        } catch (InsufficientBalanceException e) {
            walletRequest.setOperatorResponseStatus(ResponseCodes.Status.SC_INSUFFICIENT_FUNDS);
            this.setForceSuccessErrorMessageAndLog(walletRequest, e);

        } finally {
            //if status is not 1, and operatorData is not null, which mean operator process request failed
            walletRequest.setBetEnd(System.currentTimeMillis());
        }
        return walletRequest;
    }

    //force success
    private void setForceSuccessErrorMessageAndLog(WalletRequest walletRequest, Exception e) {
        if (StringUtil.isBlank(walletRequest.getErrorMessage())) {
            walletRequest.setErrorMessage(e.toString());
        }
        this.createBetResultRetryLog(walletRequest);
        this.doForceSuccessParameters(walletRequest);
    }

    @Override
    public WalletRequest debitRefundByExternalTransactionId(WalletRequest walletRequest) throws
            InternalServerException, InsufficientBalanceException, InvalidOperatorResponseException,
            BetNotFoundException, BetNotAllowedException {

        return walletBetDebitRefundProcessor.process(walletRequest);
    }

    private void createBetResultRetryLog(WalletRequest walletRequest) {
        betResultRetryLogService.create(walletRequest.getOperatorData(), walletRequest.getVendorId(), walletRequest.getAgentId(), walletRequest.getBetId(), walletRequest.getRoundId(), walletRequest.getTransactionId(), EndPoints.WALLET_BET_CREDIT);
    }

    private void doForceSuccessParameters(WalletRequest walletRequest) {
        walletRequest.setBalanceAfter(BigDecimal.ZERO);
        walletRequest.setOperatorResponseStatus(ResponseCodes.Status.SC_OK);
        walletRequest.setStatus(ResponseCodes.Status.SC_OK.code);
    }
}
