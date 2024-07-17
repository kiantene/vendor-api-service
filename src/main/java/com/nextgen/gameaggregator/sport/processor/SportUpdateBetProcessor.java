package com.nextgen.gameaggregator.sport.processor;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.SportMasterUnsettledBetMariaDB;
import com.nextgen.gameaggregator.entity.ga.SportUnsettledBetMariaDB;
import com.nextgen.gameaggregator.entity.ga.VendorCurrency;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.sport.updatebet.SportUpdateBetAction;
import com.nextgen.gameaggregator.operator.sport.updatebet.SportUpdateBetDto;
import com.nextgen.gameaggregator.operator.sport.updatebet.UpdateBetWalletRequest;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
import com.nextgen.gameaggregator.service.BetResultRetryLogService;
import com.nextgen.gameaggregator.service.KafkaService;
import com.nextgen.gameaggregator.service.VendorCurrencyService;
import com.nextgen.gameaggregator.sport.entity.SportUnsettledBet;
import com.nextgen.gameaggregator.sport.service.SportUnsettledBetService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
public class SportUpdateBetProcessor {

    private final SportUnsettledBetService sportUnsettledBetService;
    private final SportUpdateBetAction sportUpdateBetAction;
    private final KafkaService kafkaService;
    private final VendorCurrencyService vendorCurrencyService;
    private final BetResultRetryLogService betResultRetryLogService;
    private final WalletRequestService walletRequestService;

    @Autowired
    public SportUpdateBetProcessor(SportUnsettledBetService sportUnsettledBetService,
                                   SportUpdateBetAction sportUpdateBetAction,
                                   KafkaService kafkaService,
                                   VendorCurrencyService vendorCurrencyService,
                                   BetResultRetryLogService betResultRetryLogService,
                                   WalletRequestService walletRequestService) {

        this.sportUnsettledBetService = sportUnsettledBetService;
        this.sportUpdateBetAction = sportUpdateBetAction;
        this.kafkaService = kafkaService;
        this.vendorCurrencyService = vendorCurrencyService;
        this.betResultRetryLogService = betResultRetryLogService;
        this.walletRequestService = walletRequestService;
    }

    public WalletRequest process(WalletRequest walletRequest) throws
            BetResultIdempotentViolationException, TransactionStillProcessingException, InvalidOperatorResponseException,
            BetNotFoundException, BetNotAllowedException, InvalidRequestException, InvalidPlayerException {

        walletRequest.setBetStart(System.currentTimeMillis());

        // validate walletRequest
        ValidationUtils.doSportProcessorValidation(new UpdateBetWalletRequest(walletRequest));

        String vendorPlayerUsername = walletRequest.getVendorPlayerUsername();
        BigDecimal balance = BigDecimal.ZERO;
        BigDecimal fromVendorRate;
        BigDecimal toVendorRate;
        Integer defaultResponses = ResponseCodes.Status.SC_OK.code;

        walletRequestService.updateByVendorUsername(walletRequest, vendorPlayerUsername);

        SportUnsettledBet unsettledBet = Optional.ofNullable(sportUnsettledBetService.idempotentCheck(walletRequest))
                .orElseThrow(() -> new BetNotFoundException("Sport confirm bet idempotent check failed"));

        this.updateUnsettleBet(walletRequest, unsettledBet);

        // prepare bet data for wallet request for confirm bet api request.
        this.prepareBetDataForWalletRequest(walletRequest, unsettledBet);

        try {
            VendorCurrency vendorCurrency = vendorCurrencyService.findByVendorIdAndCurrencyId(walletRequest.getVendorId(), unsettledBet.getCurrencyId());
            fromVendorRate = vendorCurrency.getFromVendorRate();
            toVendorRate = vendorCurrency.getToVendorRate();

        } catch (VendorCurrencyNotSupportException exception) {
            // this should not happen but will log the error if it does
            log.error(exception.getMessage());
            throw new BetNotAllowedException(exception.getClass().getSimpleName());
        }

        try {
            SportUpdateBetDto dto = new SportUpdateBetDto(walletRequest, fromVendorRate);
            WalletBalanceVo walletBalanceVo = sportUpdateBetAction.callToOperator(walletRequest, dto);

            balance = walletRequestService.convertAmountToVendorRate(walletBalanceVo, toVendorRate);
            walletRequest.setBalanceAfter(balance);

        } catch (InvalidOperatorResponseException e) {
            defaultResponses = e.getOperatorStatus();

        } catch (Exception e) {
            defaultResponses = ResponseCodes.Status.SC_UNKNOWN_ERROR.code;

        } finally {
            if (!defaultResponses.equals(ResponseCodes.Status.SC_OK.code)) {
                // create result retry log data and prepare for message resend
                betResultRetryLogService.create(walletRequest.getOperatorData(), walletRequest.getVendorId(), walletRequest.getAgentId(), unsettledBet.getBetId(), unsettledBet.getRoundId(), unsettledBet.getInternalTransactionId(), EndPoints.SPORT_UPDATE_BET);
            }

            // Update record in sport_unsettled_bet (Couchbase)
            unsettledBet.setStatus(ResponseCodes.Status.SC_OK.code);
            unsettledBet.setOperatorStatus(ResponseCodes.Status.SC_OK.code);
            unsettledBet.setExternalTransactionId(walletRequest.getExternalTransactionId());
            unsettledBet.setBalance(balance);
            unsettledBet.setIsConfirmBet(1);
            sportUnsettledBetService.save(unsettledBet);

            // Update record in sport_unsettled_bet (MariaDB)
            SportUnsettledBetMariaDB sportUnsettledBetMariaDB = new SportUnsettledBetMariaDB(unsettledBet);
            sportUnsettledBetMariaDB.setStatus(0);
            kafkaService.produceUnsettledBet(sportUnsettledBetMariaDB, fromVendorRate);

            // update master Unsettle bet if multiple bet
            if (Objects.nonNull(unsettledBet.getMasterSportUnsettleBetId())) {
                Optional<SportUnsettledBet> sportMasterUnsettledBetOptional = sportUnsettledBetService.getById(unsettledBet.getMasterSportUnsettleBetId());
                if (sportMasterUnsettledBetOptional.isPresent()) {
                    // Update record in sport_master_unsettled_bet (Couchbase)
                    SportUnsettledBet masterUnsettledBet = sportMasterUnsettledBetOptional.get();
                    masterUnsettledBet.setStatus(ResponseCodes.Status.SC_OK.code);
                    masterUnsettledBet.setOperatorStatus(ResponseCodes.Status.SC_OK.code);
                    masterUnsettledBet.setBalance(balance);
                    masterUnsettledBet.setIsConfirmBet(1);
                    sportUnsettledBetService.save(masterUnsettledBet);

                    // Update record in sport_master_unsettled_bet (MariaDB)
                    SportMasterUnsettledBetMariaDB sportMasterUnsettledBetMariaDB = new SportMasterUnsettledBetMariaDB(masterUnsettledBet);
                    sportMasterUnsettledBetMariaDB.setStatus(BetStatus.UNSETTLED.code);
                    kafkaService.produceMasterUnsettledBet(sportMasterUnsettledBetMariaDB, fromVendorRate);
                }
            }

            walletRequest.setBetEnd(System.currentTimeMillis());
        }

        return walletRequest;
    }

    private void prepareBetDataForWalletRequest(WalletRequest walletRequest, SportUnsettledBet unsettledBet) throws BetNotAllowedException {

        // prepare betAmount
        if (walletRequest.getBetAmount() == null) {
            walletRequest.setBetAmount(unsettledBet.getBetAmount());
        }

        // prepare newBetAmount
        if (walletRequest.getNewBetAmount() == null) {
            walletRequest.setNewBetAmount(walletRequest.getBetAmount());
        }
        unsettledBet.setNewBetAmount(walletRequest.getNewBetAmount());

        // prepare effectiveTurnover
        if (walletRequest.getEffectiveTurnover() == null) {
            walletRequest.setEffectiveTurnover(walletRequest.getNewBetAmount());
        }
        unsettledBet.setEffectiveTurnover(walletRequest.getEffectiveTurnover());

        walletRequest.setVendorBetTime(unsettledBet.getVendorBetTime());
        walletRequest.setBetId(unsettledBet.getBetId());

        // check and verify vendor currency and vendor game
        walletRequestService.updateByVendorGameId(walletRequest, unsettledBet.getVendorGameId());
        walletRequestService.updateByCurrencyId(walletRequest, unsettledBet.getCurrencyId());
    }

    private void updateUnsettleBet(WalletRequest walletRequest, SportUnsettledBet unsettledBet) {
        unsettledBet.setVendorBetId(Objects.requireNonNullElse(walletRequest.getNewVendorBetId(), walletRequest.getVendorBetId()));
        unsettledBet.setRoundId(Objects.requireNonNullElse(walletRequest.getNewRoundId(), walletRequest.getRoundId()));

        // if idempotent check is passed then set internalTransactionId as new traceId
        if (unsettledBet.getStatus().equals(ResponseCodes.Status.SC_OK.code)) {
            unsettledBet.setInternalTransactionId(walletRequest.getTraceId());
        }
        unsettledBet.setStatus(ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code);
    }
}
