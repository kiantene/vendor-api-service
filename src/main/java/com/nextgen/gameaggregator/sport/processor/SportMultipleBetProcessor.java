package com.nextgen.gameaggregator.sport.processor;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.entity.ga.SportMasterUnsettledBetMariaDB;
import com.nextgen.gameaggregator.entity.ga.SportUnsettledBetMariaDB;
import com.nextgen.gameaggregator.entity.ga.VendorCurrency;
import com.nextgen.gameaggregator.entity.ga.VendorGame;
import com.nextgen.gameaggregator.exception.BetResultIdempotentViolationException;
import com.nextgen.gameaggregator.exception.InsufficientBalanceException;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.exception.TransactionStillProcessingException;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.dto.MultipleBetDto;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.operator.sport.bet.SportBetAction;
import com.nextgen.gameaggregator.service.KafkaService;
import com.nextgen.gameaggregator.service.VendorCurrencyService;
import com.nextgen.gameaggregator.service.VendorGameService;
import com.nextgen.gameaggregator.sport.entity.SportUnsettledBet;
import com.nextgen.gameaggregator.sport.service.SportUnsettledBetService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class SportMultipleBetProcessor {

    private final SportBetAction sportBetAction;
    private final SportUnsettledBetService sportUnsettledBetService;
    private final VendorCurrencyService vendorCurrencyService;
    private final KafkaService kafkaService;
    private final VendorGameService vendorGameService;

    public SportMultipleBetProcessor(SportBetAction sportBetAction,
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

    public WalletRequest process(WalletRequest walletRequest)
            throws BetResultIdempotentViolationException, TransactionStillProcessingException,
            InsufficientBalanceException, InvalidOperatorResponseException {

        walletRequest.setBetStart(System.currentTimeMillis());
        walletRequest.setResultType(ResultType.BET.code);

        Integer vendorId = walletRequest.getVendorId();
        Integer currencyId = walletRequest.getCurrencyId();
        Integer vendorGameId = walletRequest.getVendorGameId();

        sportUnsettledBetService.idempotentCheck(walletRequest);
        SportUnsettledBet sportMasterUnsettledBet = new SportUnsettledBet(walletRequest);
        sportMasterUnsettledBet.setStatus(ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code);

        List<MultipleBetDto> betList = walletRequest.getBetIds();
        List<SportUnsettledBet> sportUnsettledBetList = this.buildUnsettledBetList(walletRequest, betList, sportMasterUnsettledBet.getId());

        try {
            VendorCurrency vendorCurrency = vendorCurrencyService.findByVendorIdAndCurrencyId(vendorId, currencyId);
            VendorGame vendorGame = vendorGameService.getByVendorGameId(vendorGameId);
            walletRequest.setGameCode(vendorGame.getCode());

            walletRequest = sportBetAction.callToOperator(walletRequest, vendorCurrency);
            BigDecimal balance = walletRequest.getBalanceAfter();
            sportUnsettledBetList.forEach(
                    sportUnsettledBetCouchbase -> {
                        sportUnsettledBetCouchbase.setOperatorStatus(ResponseCodes.Status.SC_OK.code);
                        sportUnsettledBetCouchbase.setBalance(balance);
                        sportUnsettledBetCouchbase.setStatus(ResponseCodes.Status.SC_OK.code);
                        sportUnsettledBetService.save(sportUnsettledBetCouchbase);
                        SportUnsettledBetMariaDB sportUnsettledBetMariaDB = new SportUnsettledBetMariaDB(sportUnsettledBetCouchbase);
                        sportUnsettledBetMariaDB.setStatus(0);
                        kafkaService.produceUnsettledBet(sportUnsettledBetMariaDB, vendorCurrency.getFromVendorRate());
                    }
            );

            sportMasterUnsettledBet.setOperatorStatus(ResponseCodes.Status.SC_OK.code);
            sportMasterUnsettledBet.setBalance(balance);
            sportMasterUnsettledBet.setStatus(ResponseCodes.Status.SC_OK.code);
            sportUnsettledBetService.save(sportMasterUnsettledBet);
            SportMasterUnsettledBetMariaDB sportMasterUnsettledBetMariaDB = new SportMasterUnsettledBetMariaDB(sportMasterUnsettledBet);
            sportMasterUnsettledBetMariaDB.setStatus(0);
            kafkaService.produceMasterUnsettledBet(sportMasterUnsettledBetMariaDB, vendorCurrency.getFromVendorRate());

        } catch (InsufficientBalanceException e) {
            Integer operatorStatus = e.getOperatorStatus();
            sportUnsettledBetList.forEach(
                    sportUnsettledBetCouchbase -> {
                        sportUnsettledBetCouchbase.setOperatorStatus(operatorStatus);
                        sportUnsettledBetService.save(sportUnsettledBetCouchbase);
                    }
            );
            sportUnsettledBetService.save(sportMasterUnsettledBet);
            throw e;

        } catch (InvalidOperatorResponseException e) {

            // record status code from operator if they return an error
            Integer operatorStatus = e.getOperatorStatus();
            sportUnsettledBetList.forEach(
                    sportUnsettledBetCouchbase -> {
                        sportUnsettledBetCouchbase.setOperatorStatus(operatorStatus);
                        sportUnsettledBetService.save(sportUnsettledBetCouchbase);
                    }
            );
            sportUnsettledBetService.save(sportMasterUnsettledBet);
            throw e;

        } catch (Exception e) {
            sportUnsettledBetList.forEach(
                    sportUnsettledBetCouchbase -> {
                        sportUnsettledBetCouchbase.setOperatorStatus(ResponseCodes.Status.SC_UNKNOWN_ERROR.code);
                        sportUnsettledBetService.save(sportUnsettledBetCouchbase);
                    }
            );
            sportUnsettledBetService.save(sportMasterUnsettledBet);
            throw new InvalidOperatorResponseException(e.getMessage());

        } finally {
            walletRequest.setBetEnd(System.currentTimeMillis());
        }
        return walletRequest;
    }

    private List<SportUnsettledBet> buildUnsettledBetList(WalletRequest walletRequest, List<MultipleBetDto> betList, String masterId) {
        String vendorPlayerUsername = walletRequest.getVendorPlayerUsername();
        List<SportUnsettledBet> sportUnsettledBetList = new ArrayList<>(betList.size());

        for (MultipleBetDto multipleBetDto : betList) {
            SportUnsettledBet sportUnsettledBet = new SportUnsettledBet(walletRequest);
            sportUnsettledBet.setId(vendorPlayerUsername + '_' + multipleBetDto.getVendorBetId());
            sportUnsettledBet.setMasterSportUnsettleBetId(masterId);
            sportUnsettledBet.setBetId(multipleBetDto.getBetId());
            sportUnsettledBet.setExternalTransactionId(multipleBetDto.getExternalTransactionId());
            sportUnsettledBet.setVendorBetId(multipleBetDto.getVendorBetId());
            sportUnsettledBet.setBetAmount(multipleBetDto.getBetAmount());
            sportUnsettledBet.setNewBetAmount(multipleBetDto.getBetAmount());
            sportUnsettledBet.setStatus(ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code);
            sportUnsettledBetList.add(sportUnsettledBet);
        }
        return sportUnsettledBetList;
    }
}
