package com.nextgen.gameaggregator.sport.processor;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.BetHistory;
import com.nextgen.gameaggregator.entity.ga.SportMasterUnsettledBetMariaDB;
import com.nextgen.gameaggregator.entity.ga.SportUnsettledBetMariaDB;
import com.nextgen.gameaggregator.entity.ga.VendorCurrency;
import com.nextgen.gameaggregator.enums.BetResultType;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.sport.refund.RefundWalletRequest;
import com.nextgen.gameaggregator.operator.sport.refund.SportRefundAction;
import com.nextgen.gameaggregator.operator.sport.refund.SportRefundDto;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
import com.nextgen.gameaggregator.service.BetResultRetryLogService;
import com.nextgen.gameaggregator.service.KafkaService;
import com.nextgen.gameaggregator.service.VendorCurrencyService;
import com.nextgen.gameaggregator.sport.entity.SportSettledBet;
import com.nextgen.gameaggregator.sport.entity.SportUnsettledBet;
import com.nextgen.gameaggregator.sport.service.SportSettledBetService;
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
public class SportRefundProcessor {

    private final SportUnsettledBetService sportUnsettledBetService;
    private final SportSettledBetService sportSettledBetService;
    private final SportRefundAction sportRefundAction;
    private final KafkaService kafkaService;
    private final VendorCurrencyService vendorCurrencyService;
    private final BetResultRetryLogService betResultRetryLogService;
    private final WalletRequestService walletRequestService;

    @Autowired
    public SportRefundProcessor(SportUnsettledBetService sportUnsettledBetService,
                                SportSettledBetService sportSettledBetService,
                                SportRefundAction sportRefundAction,
                                KafkaService kafkaService,
                                VendorCurrencyService vendorCurrencyService,
                                BetResultRetryLogService betResultRetryLogService,
                                WalletRequestService walletRequestService) {

        this.sportUnsettledBetService = sportUnsettledBetService;
        this.sportSettledBetService = sportSettledBetService;
        this.sportRefundAction = sportRefundAction;
        this.kafkaService = kafkaService;
        this.vendorCurrencyService = vendorCurrencyService;
        this.betResultRetryLogService = betResultRetryLogService;
        this.walletRequestService = walletRequestService;
    }

    public WalletRequest process(WalletRequest walletRequest) throws
            BetNotFoundException, BetNotAllowedException, InvalidOperatorResponseException,
            BetResultIdempotentViolationException, InvalidPlayerException, InvalidRequestException {

        walletRequest.setBetStart(System.currentTimeMillis());

        // validate walletRequest
        ValidationUtils.doSportProcessorValidation(new RefundWalletRequest(walletRequest));

        String vendorPlayerUsername = walletRequest.getVendorPlayerUsername();
        walletRequestService.updateByVendorUsername(walletRequest, vendorPlayerUsername);

        String agentPlayerUsername = walletRequest.getOperatorUsername();
        String vendorBetId = walletRequest.getVendorBetId();
        Integer vendorId = walletRequest.getVendorId();
        BigDecimal fromVendorRate;
        BigDecimal toVendorRate;
        Integer resettleNum = 0;
        Integer unsettleResettleNum = 0;

        // idempotentCheck
        SportUnsettledBet sportUnsettledBet = sportUnsettledBetService.getByVendorPlayerUsernameAndVendorBetId(vendorPlayerUsername, vendorBetId);

        try {
            try {
                //idempotent checking on couchbase sport_settled_bet collection
                SportSettledBet sportSettledBet = sportSettledBetService.getByVendorPlayerUsernameAndVendorBetId(vendorPlayerUsername, vendorBetId);

                //check is idempotent when externalTransactionId is matched
                if (sportSettledBet.getExternalTransactionId().equals(walletRequest.getExternalTransactionId())) {
                    if (sportSettledBet.getStatus().equals(ResponseCodes.Status.SC_OK.code)) {
                        throw new BetResultIdempotentViolationException("Process refund idempotent: " + walletRequest.getVendorPlayerUsername() + '_' + walletRequest.getExternalTransactionId());
                    } else {
                        sportUnsettledBet.setInternalTransactionId(sportSettledBet.getInternalTransactionId());
                        walletRequest.setTransactionId(sportSettledBet.getInternalTransactionId());
                    }

                } else {
                    //if settledBet is found but externalTransactionId is not matched, then is new status changed of this bet
                    resettleNum = sportSettledBet.getResettleNum() + 1;
                    unsettleResettleNum = sportSettledBet.getUnsettledResettleNum();
                }

            } catch (BetNotFoundException e) {
                //If the bet is not found in sportSettledBet, then the bet should continue and settle as usual.
            }

            sportUnsettledBet.setStatus(ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code);

            Integer currencyId = sportUnsettledBet.getCurrencyId();

            walletRequestService.updateByVendorGameId(walletRequest, sportUnsettledBet.getVendorGameId());
            walletRequestService.updateByCurrencyId(walletRequest, currencyId);
            VendorCurrency vendorCurrency = vendorCurrencyService.findByVendorIdAndCurrencyId(vendorId, currencyId);
            fromVendorRate = vendorCurrency.getFromVendorRate();
            toVendorRate = vendorCurrency.getToVendorRate();
            walletRequest.setBetId(sportUnsettledBet.getBetId());
            walletRequest.setRoundId(sportUnsettledBet.getRoundId());

        } catch (VendorCurrencyNotSupportException exception) {
            // this should not happen but will log the error if it does
            log.error(exception.getMessage());
            throw new BetNotFoundException(exception.getMessage());
        }

        try {
            SportRefundDto dto = new SportRefundDto(walletRequest);
            WalletBalanceVo walletBalanceVo = sportRefundAction.callToOperator(walletRequest, dto);
            BigDecimal balance = walletRequestService.convertAmountToVendorRate(walletBalanceVo, toVendorRate);
            walletRequest.setBalanceAfter(balance);
            sportUnsettledBet.setOperatorStatus(ResponseCodes.Status.SC_OK.code);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            log.error(invalidOperatorResponseException.getMessage());
            sportUnsettledBet.setOperatorStatus(invalidOperatorResponseException.getOperatorStatus());

            // initiate retry
            betResultRetryLogService.create(walletRequest.getOperatorData(), walletRequest.getVendorId(), walletRequest.getAgentId(), sportUnsettledBet.getBetId(), sportUnsettledBet.getRoundId(), sportUnsettledBet.getInternalTransactionId(), EndPoints.SPORT_REFUND);
        } finally {
            walletRequest.setBetEnd(System.currentTimeMillis());
        }

        sportUnsettledBet.setBalance(walletRequest.getBalanceAfter());
        sportUnsettledBet.setStatus(ResponseCodes.Status.SC_OK.code);
        sportUnsettledBet.setEffectiveTurnover(Objects.requireNonNullElse(sportUnsettledBet.getNewBetAmount(), sportUnsettledBet.getBetAmount()));
        sportUnsettledBet.setResettleNum(resettleNum);
        sportUnsettledBet.setUnsettledResettleNum(unsettleResettleNum);

        this.sendToKafka(sportUnsettledBet, agentPlayerUsername, vendorPlayerUsername, fromVendorRate);

        // Insert record into sport_settled_bet (Couchbase)
        sportSettledBetService.save(new SportSettledBet(sportUnsettledBet));

        // Delete record in sport_unsettled_bet (Couchbase)
        sportUnsettledBetService.delete(sportUnsettledBet);

        this.updateUnsettleBetStatus(sportUnsettledBet, fromVendorRate);

        this.updateMasterBetRecord(sportUnsettledBet, fromVendorRate);

        walletRequest.setBetEnd(System.currentTimeMillis());

        return walletRequest;
    }

    private void sendToKafka(SportUnsettledBet unsettledBet, String agentPlayerUsername, String vendorPlayerUsername, BigDecimal fromVendorRate) {
        BetHistory betHistory = unsettledBet.toBetHistory(BetStatus.REFUNDED.code, BetResultType.BET.code);

        kafkaService.produceBetHistory(betHistory, vendorPlayerUsername, fromVendorRate);
        kafkaService.produceWarehouseBetHistory(betHistory, agentPlayerUsername, vendorPlayerUsername, fromVendorRate);
    }

    private void updateUnsettleBetStatus(SportUnsettledBet unsettledBet, BigDecimal fromVendorRate) {
        // Update status in sport_unsettled_bet (MariaDB) so that it doesn't show up in BO
        SportUnsettledBetMariaDB sportUnsettledBetMariaDB = new SportUnsettledBetMariaDB(unsettledBet);
        sportUnsettledBetMariaDB.setResettleNum(unsettledBet.getUnsettledResettleNum());
        kafkaService.produceUnsettledBet(sportUnsettledBetMariaDB, fromVendorRate);
    }

    private void updateMasterBetRecord(SportUnsettledBet unsettledBet, BigDecimal fromVendorRate) {
        // update master Unsettle bet if multiple bet
        if (Objects.nonNull(unsettledBet.getMasterSportUnsettleBetId())) {
            Optional<SportUnsettledBet> sportMasterUnsettledBetOptional = sportUnsettledBetService.getById(unsettledBet.getMasterSportUnsettleBetId());
            if (sportMasterUnsettledBetOptional.isPresent()) {
                // Delete record in sport_unsettled_bet (Couchbase)
                SportUnsettledBet masterUnsettledBet = sportMasterUnsettledBetOptional.get();
                sportUnsettledBetService.delete(masterUnsettledBet);

                // Update record in sport_master_unsettled_bet (MariaDB)
                SportMasterUnsettledBetMariaDB sportMasterUnsettledBetMariaDB = new SportMasterUnsettledBetMariaDB(masterUnsettledBet);
                sportMasterUnsettledBetMariaDB.setStatus(BetStatus.SETTLED.code);
                kafkaService.produceMasterUnsettledBet(sportMasterUnsettledBetMariaDB, fromVendorRate);
            }
        }
    }
}
