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
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.sport.settle.SettleWalletRequest;
import com.nextgen.gameaggregator.operator.sport.settle.SportSettleAction;
import com.nextgen.gameaggregator.operator.sport.settle.SportSettleDto;
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

@Service
public class SportSettleBetProcessor {

    private final SportUnsettledBetService sportUnsettledBetService;
    private final SportSettledBetService sportSettledBetService;
    private final VendorCurrencyService vendorCurrencyService;
    private final SportSettleAction sportSettleAction;
    private final KafkaService kafkaService;
    private final WalletRequestService walletRequestService;

    @Autowired
    public SportSettleBetProcessor(SportUnsettledBetService sportUnsettledBetService,
                                   SportSettledBetService sportSettledBetService,
                                   VendorCurrencyService vendorCurrencyService,
                                   SportSettleAction sportSettleAction,
                                   KafkaService kafkaService,
                                   WalletRequestService walletRequestService) {

        this.sportUnsettledBetService = sportUnsettledBetService;
        this.sportSettledBetService = sportSettledBetService;
        this.vendorCurrencyService = vendorCurrencyService;
        this.sportSettleAction = sportSettleAction;
        this.kafkaService = kafkaService;
        this.walletRequestService = walletRequestService;
    }

    public WalletRequest process(WalletRequest walletRequest) throws
            BetNotFoundException, BetNotAllowedException, BetResultIdempotentViolationException,
            InvalidOperatorResponseException, TransactionStillProcessingException, InvalidRequestException, InvalidPlayerException {

        walletRequest.setBetStart(System.currentTimeMillis());

        // validate walletRequest
        ValidationUtils.doSportProcessorValidation(new SettleWalletRequest(walletRequest));

        String vendorPlayerUsername = walletRequest.getVendorPlayerUsername();
        walletRequestService.updateByVendorUsername(walletRequest, vendorPlayerUsername);

        Integer defaultResponses = ResponseCodes.Status.SC_OK.code;
        String agentPlayerUsername = walletRequest.getOperatorUsername();
        String vendorBetId = walletRequest.getVendorBetId();
        Integer vendorId = walletRequest.getVendorId();
        BigDecimal winAmount = walletRequest.getWinAmount();
        BigDecimal balance = BigDecimal.ZERO;
        BigDecimal fromVendorRate;
        BigDecimal toVendorRate;
        int resultType = winAmount.compareTo(BigDecimal.ZERO) > 0 ? BetResultType.WIN.code : BetResultType.LOSE.code;

        SportUnsettledBet sportUnsettledBet = sportUnsettledBetService.getByVendorPlayerUsernameAndVendorBetId(vendorPlayerUsername, vendorBetId);
        Integer currencyId = sportUnsettledBet.getCurrencyId();

        this.updateWalletRequest(walletRequest, sportUnsettledBet);
        this.updateUnsettleBet(walletRequest, sportUnsettledBet);

        Integer resettleNum = 0;
        Integer unsettleResettleNum = 0;

        SportSettledBet sportSettledBet = sportSettledBetService.idempotentCheck(walletRequest);

        if (sportSettledBet != null) { // settled bet found but with different externalTransactionId (bet status changed)
            resettleNum = sportSettledBet.getResettleNum() + 1;
            unsettleResettleNum = sportSettledBet.getUnsettledResettleNum();
            sportUnsettledBet.setInternalTransactionId(sportSettledBet.getInternalTransactionId());
        }

        // check and verify vendor currency and vendor game
        walletRequestService.updateByVendorGameId(walletRequest, sportUnsettledBet.getVendorGameId());
        walletRequestService.updateByCurrencyId(walletRequest, currencyId);

        try {

            VendorCurrency vendorCurrency = vendorCurrencyService.findByVendorIdAndCurrencyId(vendorId, currencyId);
            fromVendorRate = vendorCurrency.getFromVendorRate();
            toVendorRate = vendorCurrency.getToVendorRate();

            SportSettleDto dto = new SportSettleDto(walletRequest, fromVendorRate);
            WalletBalanceVo walletBalanceVo = sportSettleAction.callToOperator(walletRequest, dto);

            balance = walletRequestService.convertAmountToVendorRate(walletBalanceVo, toVendorRate);
            walletRequest.setBalanceAfter(balance);

            // Insert settled bet into bet_history (MariaDB)
            Integer betStatus = BetStatus.SETTLED.code;
            sportUnsettledBet.setStatus(ResponseCodes.Status.SC_OK.code);
            sportUnsettledBet.setResettleNum(resettleNum);
            sportUnsettledBet.setUnsettledResettleNum(unsettleResettleNum);

            // Insert record bet_history (MariaDB)
            BetHistory betHistory = sportUnsettledBet.toBetHistory(betStatus, resultType);
            kafkaService.produceBetHistory(betHistory, null, fromVendorRate);

            kafkaService.produceWarehouseBetHistory(betHistory, agentPlayerUsername, vendorPlayerUsername, fromVendorRate);

            // Update status in sport_unsettled_bet (MariaDB)
            SportUnsettledBetMariaDB sportUnsettledBetMariaDB = new SportUnsettledBetMariaDB(sportUnsettledBet);
            sportUnsettledBetMariaDB.setStatus(ResponseCodes.Status.SC_OK.code);
            sportUnsettledBetMariaDB.setResettleNum(unsettleResettleNum);
            kafkaService.produceUnsettledBet(sportUnsettledBetMariaDB, fromVendorRate);

            // Insert record into sport_settled_bet (Couchbase)
            SportSettledBet updatedSportSettledBet = new SportSettledBet(sportUnsettledBet);
            sportSettledBetService.save(updatedSportSettledBet);

            // Delete record in sport_unsettled_bet (Couchbase)
            sportUnsettledBetService.delete(sportUnsettledBet);

            // update master Unsettle bet if multiple bet
            if (Objects.nonNull(sportUnsettledBet.getMasterSportUnsettleBetId())) {
                Optional<SportUnsettledBet> sportMasterUnsettledBetOptional = sportUnsettledBetService.getById(sportUnsettledBet.getMasterSportUnsettleBetId());
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
        } catch (InvalidOperatorResponseException e) {
            defaultResponses = e.getOperatorStatus();

        } catch (Exception e) {
            sportUnsettledBet.setOperatorStatus(ResponseCodes.Status.SC_UNKNOWN_ERROR.code);
            sportUnsettledBetService.save(sportUnsettledBet);
            sportSettledBetService.save(new SportSettledBet(sportUnsettledBet));
            throw new InvalidOperatorResponseException(e.getMessage());

        } finally {

            walletRequest.setBetEnd(System.currentTimeMillis());
        }

        return walletRequest;
    }

    private void updateWalletRequest(WalletRequest walletRequest, SportUnsettledBet unsettledBet) {
        walletRequest.setBetId(unsettledBet.getBetId());

        if (Objects.isNull(walletRequest.getBetAmount())) {
            walletRequest.setBetAmount(unsettledBet.getBetAmount());
        }

        if (Objects.isNull(walletRequest.getNewBetAmount())) {
            walletRequest.setNewBetAmount(unsettledBet.getNewBetAmount());
        }

        if (Objects.isNull(walletRequest.getEffectiveTurnover())) {
            walletRequest.setEffectiveTurnover(unsettledBet.getNewBetAmount());
        }

        if (Objects.isNull(walletRequest.getWinLoss())) {
            walletRequest.setWinLoss(walletRequest.getWinAmount().subtract(walletRequest.getNewBetAmount()));
        }
    }

    private void updateUnsettleBet(WalletRequest walletRequest, SportUnsettledBet unsettledBet) {

        unsettledBet.setInternalTransactionId(walletRequest.getTraceId());
        unsettledBet.setExternalTransactionId(walletRequest.getExternalTransactionId());
        unsettledBet.setStatus(ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code);
        unsettledBet.setWinAmount(walletRequest.getWinAmount());
        unsettledBet.setWinLoss(walletRequest.getWinLoss());
        unsettledBet.setEffectiveTurnover(walletRequest.getEffectiveTurnover());
        unsettledBet.setVendorSettleTime(Objects.requireNonNullElse(walletRequest.getVendorSettleTime(), System.currentTimeMillis()));
        unsettledBet.setResultTime(unsettledBet.getVendorSettleTime());
    }
}
