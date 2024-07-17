package com.nextgen.gameaggregator.sport.processor;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.BetHistory;
import com.nextgen.gameaggregator.entity.ga.SportUnsettledBetMariaDB;
import com.nextgen.gameaggregator.entity.ga.VendorCurrency;
import com.nextgen.gameaggregator.enums.BetResultType;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.sport.unsettle.SportUnsettleAction;
import com.nextgen.gameaggregator.operator.sport.unsettle.SportUnsettleDto;
import com.nextgen.gameaggregator.operator.sport.unsettle.UnsettleWalletRequest;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
import com.nextgen.gameaggregator.service.KafkaService;
import com.nextgen.gameaggregator.service.VendorCurrencyService;
import com.nextgen.gameaggregator.sport.entity.SportSettledBet;
import com.nextgen.gameaggregator.sport.entity.SportUnsettledBet;
import com.nextgen.gameaggregator.sport.service.SportSettledBetService;
import com.nextgen.gameaggregator.sport.service.SportUnsettledBetService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class SportUnsettleBetProcessor {

    private final SportUnsettledBetService sportUnsettledBetService;
    private final SportSettledBetService sportSettledBetService;
    private final VendorCurrencyService vendorCurrencyService;
    private final SportUnsettleAction sportUnsettleAction;
    private final KafkaService kafkaService;
    private final WalletRequestService walletRequestService;

    @Autowired
    public SportUnsettleBetProcessor(SportUnsettledBetService sportUnsettledBetService,
                                     SportSettledBetService sportSettledBetService,
                                     VendorCurrencyService vendorCurrencyService,
                                     SportUnsettleAction sportUnsettleAction,
                                     KafkaService kafkaService,
                                     WalletRequestService walletRequestService) {

        this.sportUnsettledBetService = sportUnsettledBetService;
        this.sportSettledBetService = sportSettledBetService;
        this.vendorCurrencyService = vendorCurrencyService;
        this.sportUnsettleAction = sportUnsettleAction;
        this.kafkaService = kafkaService;
        this.walletRequestService = walletRequestService;
    }

    public WalletRequest process(WalletRequest walletRequest) throws
            BetNotFoundException, BetNotAllowedException, BetResultIdempotentViolationException,
            InvalidOperatorResponseException, InvalidRequestException, InvalidPlayerException {

        walletRequest.setBetStart(System.currentTimeMillis());

        // validate walletRequest
        ValidationUtils.doSportProcessorValidation(new UnsettleWalletRequest(walletRequest));

        String vendorPlayerUsername = walletRequest.getVendorPlayerUsername();
        walletRequestService.updateByVendorUsername(walletRequest, vendorPlayerUsername);

        String agentPlayerUsername = walletRequest.getOperatorUsername();
        String externalTransactionId = walletRequest.getExternalTransactionId();
        String internalTransactionId = UUID.randomUUID().toString();
        String vendorBetId = walletRequest.getVendorBetId();
        Integer vendorId = walletRequest.getVendorId();
        BigDecimal balance = BigDecimal.ZERO;
        BigDecimal fromVendorRate;
        BigDecimal toVendorRate;

        SportSettledBet sportSettledBet = sportSettledBetService.getByVendorPlayerUsernameAndVendorBetId(vendorPlayerUsername, vendorBetId);

        // check is idempotent when externalTransactionId is matched
        if (sportSettledBet.getExternalTransactionId().equals(externalTransactionId)) {
            if (sportSettledBet.getStatus().equals(ResponseCodes.Status.SC_OK.code)) {
                throw new BetResultIdempotentViolationException("Process unsettle idempotent: " + vendorPlayerUsername + '_' + externalTransactionId);
            } else {
                internalTransactionId = sportSettledBet.getInternalTransactionId();
            }

            // TODO: to review logic
        }

        this.updateWalletRequest(walletRequest, sportSettledBet);

        // check and verify vendor currency and vendor game
        walletRequestService.updateByVendorGameId(walletRequest, sportSettledBet.getVendorGameId());
        walletRequestService.updateByCurrencyId(walletRequest, sportSettledBet.getCurrencyId());

        try {
            SportUnsettledBet sportUnsettledBet = sportSettledBet.toSportUnsettleBetCouchbase();
            sportUnsettledBet.setExternalTransactionId(Objects.requireNonNullElse(externalTransactionId, sportUnsettledBet.getExternalTransactionId()));
            sportUnsettledBet.setInternalTransactionId(internalTransactionId);
            sportUnsettledBet.setStatus(ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code);
            Optional.ofNullable(walletRequest.getTimestamp()).ifPresent(timestamp -> {
                sportUnsettledBet.setResultTime(timestamp);
                sportUnsettledBet.setVendorSettleTime(timestamp);
            });

            Integer currencyId = sportUnsettledBet.getCurrencyId();
            VendorCurrency vendorCurrency = vendorCurrencyService.findByVendorIdAndCurrencyId(vendorId, currencyId);
            fromVendorRate = vendorCurrency.getFromVendorRate();
            toVendorRate = vendorCurrency.getToVendorRate();

            SportUnsettleDto dto = new SportUnsettleDto(walletRequest);
            WalletBalanceVo walletBalanceVo = sportUnsettleAction.callToOperator(walletRequest, dto);

            balance = walletRequestService.convertAmountToVendorRate(walletBalanceVo, toVendorRate);
            walletRequest.setBalanceAfter(balance);

            sportUnsettledBet.setOperatorStatus(ResponseCodes.Status.SC_OK.code);
            sportUnsettledBet.setBalance(balance);
            sportUnsettledBet.setResultType(BetResultType.ADJUSTMENT.code);
            sportUnsettledBet.setStatus(ResponseCodes.Status.SC_OK.code);
            sportUnsettledBet.setResettleNum((sportUnsettledBet.getResettleNum() != null && sportUnsettledBet.getResettleNum() >= 0) ? sportUnsettledBet.getResettleNum() + 1 : 0);
            sportUnsettledBet.setUnsettledResettleNum(this.getUnsettledBetResettleNum(sportSettledBet));

            // Update status in (MariaDB) sport_unsettled_bet
            SportUnsettledBetMariaDB sportUnsettledBetMariaDB = new SportUnsettledBetMariaDB(sportUnsettledBet);
            sportUnsettledBetMariaDB.setStatus(0);
            sportUnsettledBetMariaDB.setResettleNum(this.getUnsettledBetResettleNum(sportSettledBet));
            kafkaService.produceUnsettledBet(sportUnsettledBetMariaDB);

            // Generate new bet history to offset the old records
            BetHistory betHistory = this.offsetOldBetHistory(sportUnsettledBet.toBetHistory(BetStatus.CANCELLED.code, BetResultType.ADJUSTMENT.code));
            kafkaService.produceBetHistory(betHistory, null, fromVendorRate);

            kafkaService.produceWarehouseBetHistory(betHistory, agentPlayerUsername, vendorPlayerUsername, fromVendorRate);

            // update data from couchbase settled bet
            sportSettledBet.setInternalTransactionId(internalTransactionId);
            sportSettledBet.setExternalTransactionId(externalTransactionId);
            sportSettledBet.setWinAmount(BigDecimal.ZERO);
            sportSettledBet.setWinLoss(BigDecimal.ZERO);
            sportSettledBet.setEffectiveTurnover(BigDecimal.ZERO);
            sportSettledBet.setResettleNum((sportSettledBet.getResettleNum() != null && sportSettledBet.getResettleNum() >= 0) ? sportSettledBet.getResettleNum() + 1 : 0);
            sportSettledBet.setUnsettledResettleNum(sportUnsettledBet.getUnsettledResettleNum());
            sportSettledBetService.save(sportSettledBet);

            // update unsettledBet with winAmount, winLoss and effectiveTurnover = 0
            sportUnsettledBet.setWinAmount(BigDecimal.ZERO);
            sportUnsettledBet.setWinLoss(BigDecimal.ZERO);
            sportUnsettledBet.setEffectiveTurnover(BigDecimal.ZERO);
            sportUnsettledBetService.save(sportUnsettledBet);

        } catch (InvalidOperatorResponseException e) {

            // record status code from operator if they return an error
            Integer operatorStatus = e.getOperatorStatus();
            sportSettledBet.setOperatorStatus(operatorStatus);
            sportSettledBet.setInternalTransactionId(internalTransactionId);
            sportSettledBetService.save(sportSettledBet);

            // TODO: add force success and retry logic

        } catch (Exception e) {

            // TODO: to be reviewed
            sportSettledBet.setStatus(ResponseCodes.Status.SC_UNKNOWN_ERROR.code);
            sportSettledBet.setInternalTransactionId(internalTransactionId);
            sportSettledBetService.save(sportSettledBet);
            throw new InvalidOperatorResponseException(e.getMessage());

        } finally {
            walletRequest.setBetEnd(System.currentTimeMillis());
        }

        return walletRequest;
    }

    private BetHistory offsetOldBetHistory(BetHistory betHistory) {
        BigDecimal newBetAmount = Optional.ofNullable(betHistory.getBetAmount()).map(BigDecimal::negate).orElse(BigDecimal.ZERO);
        BigDecimal newWinAmount = Optional.ofNullable(betHistory.getWinAmount()).map(BigDecimal::negate).orElse(BigDecimal.ZERO);
        BigDecimal newWinLoss = Optional.ofNullable(betHistory.getWinLoss()).map(BigDecimal::negate).orElse(BigDecimal.ZERO);
        BigDecimal newEffectiveTurnover = Optional.ofNullable(betHistory.getEffectiveTurnover()).map(BigDecimal::negate).orElse(BigDecimal.ZERO);

        betHistory.setBetAmount(newBetAmount);
        betHistory.setWinAmount(newWinAmount);
        betHistory.setWinLoss(newWinLoss);
        betHistory.setEffectiveTurnover(newEffectiveTurnover);

        return betHistory;
    }

    private Integer getUnsettledBetResettleNum(SportSettledBet sportSettledBet) {
        int unsettledResettleNum = 0;

        if (sportSettledBet.getUnsettledResettleNum() != null) {
            unsettledResettleNum = sportSettledBet.getUnsettledResettleNum() + 1;
        }

        return unsettledResettleNum;
    }

    private void updateWalletRequest(WalletRequest walletRequest, SportSettledBet sportSettledBet) {
        walletRequest.setBetId(sportSettledBet.getBetId());
    }
}
