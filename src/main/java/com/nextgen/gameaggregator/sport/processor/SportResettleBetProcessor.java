package com.nextgen.gameaggregator.sport.processor;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.BetHistory;
import com.nextgen.gameaggregator.entity.ga.VendorCurrency;
import com.nextgen.gameaggregator.enums.BetResultType;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.sport.resettle.ResettleWalletRequest;
import com.nextgen.gameaggregator.operator.sport.resettle.SportResettleAction;
import com.nextgen.gameaggregator.operator.sport.resettle.SportResettleDto;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
import com.nextgen.gameaggregator.service.KafkaService;
import com.nextgen.gameaggregator.service.VendorCurrencyService;
import com.nextgen.gameaggregator.sport.entity.SportSettledBet;
import com.nextgen.gameaggregator.sport.service.SportSettledBetService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;

@Service
public class SportResettleBetProcessor {
    private final SportSettledBetService sportSettledBetService;
    private final VendorCurrencyService vendorCurrencyService;
    private final SportResettleAction sportResettleAction;
    private final KafkaService kafkaService;
    private final WalletRequestService walletRequestService;

    @Autowired
    public SportResettleBetProcessor(SportSettledBetService sportSettledBetService,
                                     VendorCurrencyService vendorCurrencyService,
                                     SportResettleAction sportResettleAction,
                                     KafkaService kafkaService,
                                     WalletRequestService walletRequestService) {

        this.sportSettledBetService = sportSettledBetService;
        this.vendorCurrencyService = vendorCurrencyService;
        this.sportResettleAction = sportResettleAction;
        this.kafkaService = kafkaService;
        this.walletRequestService = walletRequestService;
    }

    public WalletRequest process(WalletRequest walletRequest) throws
            BetNotFoundException, BetNotAllowedException, BetResultIdempotentViolationException,
            InvalidOperatorResponseException, TransactionStillProcessingException, InvalidRequestException {

        walletRequest.setBetStart(System.currentTimeMillis());

        // validate walletRequest
        ValidationUtils.doValidation(new ResettleWalletRequest(walletRequest), InvalidRequestException::new);

        String vendorPlayerUsername = walletRequest.getVendorPlayerUsername();
        String externalTransactionId = walletRequest.getExternalTransactionId();
        String vendorBetId = walletRequest.getVendorBetId();
        Integer vendorId = walletRequest.getVendorId();
        BigDecimal fromVendorRate;
        BigDecimal toVendorRate;

        String internalTransactionId = walletRequest.getTraceId();
        SportSettledBet sportSettledBet = sportSettledBetService.getByVendorPlayerUsernameAndVendorBetId(vendorPlayerUsername, vendorBetId);
        Integer currencyId = sportSettledBet.getCurrencyId();

        sportSettledBet.setStatus(ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code);
        sportSettledBet.setInternalTransactionId(internalTransactionId);
        sportSettledBet.setExternalTransactionId(Objects.requireNonNullElse(externalTransactionId, sportSettledBet.getExternalTransactionId()));

        // check and verify vendor currency and vendor game
        walletRequestService.updateByVendorGameId(walletRequest, sportSettledBet.getVendorGameId());
        walletRequestService.updateByCurrencyId(walletRequest, currencyId);

        try {
            VendorCurrency vendorCurrency = vendorCurrencyService.findByVendorIdAndCurrencyId(vendorId, currencyId);
            fromVendorRate = vendorCurrency.getFromVendorRate();
            toVendorRate = vendorCurrency.getToVendorRate();

            SportResettleDto dto = new SportResettleDto(walletRequest, fromVendorRate);
            WalletBalanceVo walletBalanceVo = sportResettleAction.callToOperator(walletRequest, dto);
            BigDecimal balance = walletRequestService.convertAmountToVendorRate(walletBalanceVo, toVendorRate);
            walletRequest.setBalanceAfter(balance);

            this.updateSettledBet(sportSettledBet, walletRequest, balance);

            this.produceBetHistory(sportSettledBet, walletRequest, fromVendorRate);

        } catch (Exception e) {
            sportSettledBet.setStatus(ResponseCodes.Status.SC_UNKNOWN_ERROR.code);
            sportSettledBetService.save(sportSettledBet);
            throw new InvalidOperatorResponseException(e.getMessage());

        } finally {

            walletRequest.setBetEnd(System.currentTimeMillis());
        }

        return walletRequest;
    }

    private void produceBetHistory(SportSettledBet sportSettledBet, WalletRequest walletRequest, BigDecimal fromVendorRate) {
        String agentPlayerUsername = walletRequest.getOperatorUsername();
        String vendorPlayerUsername = walletRequest.getVendorPlayerUsername();
        BigDecimal diffWinAmount = walletRequest.getNewWinAmount().subtract(sportSettledBet.getWinAmount());
        int resultType = diffWinAmount.compareTo(BigDecimal.ZERO) > 0 ? BetResultType.WIN.code : BetResultType.LOSE.code;

        // Generate new bet history to offset the old records
        BetHistory betHistory = sportSettledBet.toBetHistory(BetStatus.SETTLED.code, resultType);
        betHistory.setBetAmount(BigDecimal.ZERO);
        betHistory.setWinAmount(diffWinAmount);
        betHistory.setWinLoss(diffWinAmount);
        betHistory.setEffectiveTurnover(BigDecimal.ZERO);
        kafkaService.produceBetHistory(betHistory, vendorPlayerUsername, fromVendorRate);
        kafkaService.produceWarehouseBetHistory(betHistory, agentPlayerUsername, vendorPlayerUsername, fromVendorRate);
    }

    private void updateSettledBet(SportSettledBet sportSettledBet, WalletRequest walletRequest, BigDecimal balance) {
        BigDecimal winAmount = sportSettledBet.getWinAmount();
        Integer resultType = winAmount.compareTo(BigDecimal.ZERO) > 0 ? BetResultType.WIN.code : BetResultType.LOSE.code;
        int resettleNum = 0;
        if (sportSettledBet.getResettleNum() != null) {
            resettleNum += 1;
        }

        sportSettledBet.setWinAmount(walletRequest.getNewWinAmount());
        sportSettledBet.setWinLoss(sportSettledBet.getWinAmount());
        sportSettledBet.setOperatorStatus(ResponseCodes.Status.SC_OK.code);
        sportSettledBet.setStatus(ResponseCodes.Status.SC_OK.code);
        sportSettledBet.setBalance(balance);
        sportSettledBet.setResettleNum(resettleNum);
        sportSettledBet.setResultType(resultType);
        sportSettledBetService.save(sportSettledBet);
    }
}
