package com.nextgen.gameaggregator.operator.wallet.service;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestServiceImpl;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.wallet.betcredit.WalletBetCreditProcessor;
import com.nextgen.gameaggregator.operator.wallet.betdebit.WalletBetDebitProcessor;
import com.nextgen.gameaggregator.operator.wallet.rollback.WalletBetDebitRefundProcessor;
import com.nextgen.gameaggregator.service.BetResultRetryLogService;
import com.nextgen.gameaggregator.service.KafkaService;
import com.nextgen.gameaggregator.service.WalletTransactionServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class OperatorWalletServiceImpl implements OperatorWalletService {

    private final WalletBetCreditProcessor walletBetCreditProcessor;
    private final WalletBetDebitProcessor walletBetDebitProcessor;
    private final WalletBetDebitRefundProcessor walletBetDebitRefundProcessor;
    private final BetResultRetryLogService betResultRetryLogService;

    public OperatorWalletServiceImpl(WalletTransactionServiceImpl walletTransactionService,
                                     WalletRequestServiceImpl walletRequestService,
                                     KafkaService kafkaService, BetResultRetryLogService betResultRetryLogService) {

        this.betResultRetryLogService = betResultRetryLogService;
        this.walletBetCreditProcessor = new WalletBetCreditProcessor(walletRequestService, walletTransactionService, kafkaService);
        this.walletBetDebitProcessor = new WalletBetDebitProcessor(walletRequestService, walletTransactionService);
        this.walletBetDebitRefundProcessor = new WalletBetDebitRefundProcessor(walletRequestService, walletTransactionService, kafkaService);
    }

    @Override
    public WalletRequest betDebit(WalletRequest walletRequest) throws InvalidOperatorResponseException, InsufficientBalanceException, InternalServerException, BetNotAllowedException {

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
    public WalletRequest betCredit(WalletRequest walletRequest) throws InternalServerException, InsufficientBalanceException, InvalidOperatorResponseException, BetNotAllowedException {

        try {
            walletRequest = walletBetCreditProcessor.process(walletRequest);

        } catch (BetNotAllowedException | InternalServerException e) {
            //before callToOperator or while generateSettleBet and failed
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
            //if status is not 1, and operatorData is not null, which mean operator process request failed
            //then will require send retry credit request to this operator
            if (!walletRequest.getStatus().equals(ResponseCodes.Status.SC_OK.code)) {
                if (walletRequest.getOperatorData() != null) {
                    betResultRetryLogService.create(walletRequest.getOperatorData(), walletRequest.getVendorId(), walletRequest.getAgentId(), walletRequest.getBetId(), walletRequest.getRoundId(), walletRequest.getTransactionId(), EndPoints.WALLET_BET_CREDIT);
                }
            }

            walletRequest.setBetEnd(System.currentTimeMillis());
        }

        return walletRequest;
    }

    @Override
    public WalletRequest debitRefundByExternalTransactionId(WalletRequest walletRequest) throws
            InternalServerException, InsufficientBalanceException, InvalidOperatorResponseException,
            BetNotFoundException, BetNotAllowedException {

        return walletBetDebitRefundProcessor.process(walletRequest);
    }
}
