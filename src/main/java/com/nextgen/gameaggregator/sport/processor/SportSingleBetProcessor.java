package com.nextgen.gameaggregator.sport.processor;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.entity.ga.SportUnsettledBetMariaDB;
import com.nextgen.gameaggregator.entity.ga.VendorCurrency;
import com.nextgen.gameaggregator.entity.ga.VendorGame;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.operator.sport.bet.BetWalletRequest;
import com.nextgen.gameaggregator.operator.sport.bet.SportBetAction;
import com.nextgen.gameaggregator.service.KafkaService;
import com.nextgen.gameaggregator.service.VendorCurrencyService;
import com.nextgen.gameaggregator.service.VendorGameService;
import com.nextgen.gameaggregator.sport.entity.SportUnsettledBet;
import com.nextgen.gameaggregator.sport.service.SportUnsettledBetService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class SportSingleBetProcessor {

    private final SportBetAction sportBetAction;
    private final SportUnsettledBetService sportUnsettledBetService;
    private final VendorCurrencyService vendorCurrencyService;
    private final KafkaService kafkaService;
    private final VendorGameService vendorGameService;

    public SportSingleBetProcessor(SportBetAction sportBetAction,
                                   SportUnsettledBetService sportUnsettledBetService,
                                   VendorCurrencyService vendorCurrencyService,
                                   KafkaService kafkaService,
                                   VendorGameService vendorGameService) {

        this.sportBetAction = sportBetAction;
        this.sportUnsettledBetService = sportUnsettledBetService;
        this.vendorCurrencyService = vendorCurrencyService;
        this.kafkaService = kafkaService;
        this.vendorGameService = vendorGameService;
    }

    public WalletRequest process(WalletRequest walletRequest) throws
            BetResultIdempotentViolationException, TransactionStillProcessingException,
            InsufficientBalanceException, InvalidOperatorResponseException, InvalidRequestException {

        walletRequest.setBetStart(System.currentTimeMillis());
        walletRequest.setResultType(ResultType.BET.code);

        // validate walletRequest
        ValidationUtils.doValidation(new BetWalletRequest(walletRequest), InvalidRequestException::new);

        Integer vendorId = walletRequest.getVendorId();
        Integer currencyId = walletRequest.getCurrencyId();
        Integer vendorGameId = walletRequest.getVendorGameId();

        SportUnsettledBet existsUnsettledBet = sportUnsettledBetService.idempotentCheck(walletRequest);
        SportUnsettledBet unsettledBet = new SportUnsettledBet(walletRequest);
        unsettledBet.setStatus(ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code);

        if (existsUnsettledBet != null) {
            unsettledBet.setBetId(existsUnsettledBet.getBetId());
            unsettledBet.setInternalTransactionId(existsUnsettledBet.getInternalTransactionId());
        }

        try {
            VendorCurrency vendorCurrency = vendorCurrencyService.findByVendorIdAndCurrencyId(vendorId, currencyId);
            VendorGame vendorGame = vendorGameService.getByVendorGameId(vendorGameId);
            walletRequest.setGameCode(vendorGame.getCode());

            walletRequest = sportBetAction.callToOperator(walletRequest, vendorCurrency);
            BigDecimal balance = walletRequest.getBalanceAfter();
            unsettledBet.setOperatorStatus(ResponseCodes.Status.SC_OK.code);
            unsettledBet.setBalance(balance);
            unsettledBet.setStatus(ResponseCodes.Status.SC_OK.code);
            sportUnsettledBetService.save(unsettledBet);

            SportUnsettledBetMariaDB sportUnsettledBetMariaDB = new SportUnsettledBetMariaDB(unsettledBet);
            sportUnsettledBetMariaDB.setStatus(0);
            kafkaService.produceUnsettledBet(sportUnsettledBetMariaDB, vendorCurrency.getFromVendorRate());

        } catch (InsufficientBalanceException e) {
            unsettledBet.setOperatorStatus(e.getOperatorStatus());
            sportUnsettledBetService.save(unsettledBet);
            throw e;

        } catch (InvalidOperatorResponseException e) {

            unsettledBet.setOperatorStatus(e.getOperatorStatus());
            sportUnsettledBetService.save(unsettledBet);
            throw e;

        } catch (Exception e) {
            unsettledBet.setOperatorStatus(ResponseCodes.Status.SC_UNKNOWN_ERROR.code);
            sportUnsettledBetService.save(unsettledBet);
            throw new InvalidOperatorResponseException(e.getMessage());

        } finally {
            walletRequest.setBetEnd(System.currentTimeMillis());
        }

        return walletRequest;
    }
}
