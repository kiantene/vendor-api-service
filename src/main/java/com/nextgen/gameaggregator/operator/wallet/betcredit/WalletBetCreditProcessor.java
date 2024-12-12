package com.nextgen.gameaggregator.operator.wallet.betcredit;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.BetHistory;
import com.nextgen.gameaggregator.entity.ga.WalletTransaction;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.enums.BetType;
import com.nextgen.gameaggregator.exception.BetNotAllowedException;
import com.nextgen.gameaggregator.exception.InsufficientBalanceException;
import com.nextgen.gameaggregator.exception.InternalServerException;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.operator.wallet.service.OperatorWalletService;
import com.nextgen.gameaggregator.service.CurrencyConversionService;
import com.nextgen.gameaggregator.service.KafkaService;
import com.nextgen.gameaggregator.service.WalletTransactionService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class WalletBetCreditProcessor {

    private final WalletRequestService walletRequestService;
    private final WalletTransactionService walletTransactionService;
    private final KafkaService kafkaService;

    public WalletBetCreditProcessor(WalletRequestService walletRequestService,
                                    WalletTransactionService walletTransactionService,
                                    KafkaService kafkaService) {


        this.walletRequestService = walletRequestService;
        this.walletTransactionService = walletTransactionService;
        this.kafkaService = kafkaService;
    }

    public WalletRequest process(WalletRequest walletRequest) throws InternalServerException, BetNotAllowedException, InsufficientBalanceException, InvalidOperatorResponseException {

        walletRequestService.initialise(walletRequest);

        if (walletRequest.getWinLoss() == null && walletRequest.getBetAmount() != null && walletRequest.getWinAmount() != null) {
            walletRequest.setWinLoss(walletRequest.getWinAmount().subtract(walletRequest.getBetAmount()));
        }

        WalletTransaction walletTransaction = walletTransactionService.prepareEntity(walletRequest, OperatorWalletService.CREDIT);

        walletTransactionService.save(walletTransaction);

        WalletBetCreditDto dto = this.prepareOperatorRequestData(walletRequest);

        new WalletBetCreditAction().callToOperator(walletRequest, dto);

        walletTransaction.setOperatorStatus(walletRequest.getOperatorResponseStatus().code);
        walletTransactionService.save(walletTransaction);

        walletRequest = this.generateBetHistory(walletTransaction, walletRequest);

        return walletRequest;
    }

    public WalletRequest generateBetHistory(WalletTransaction walletTransaction, WalletRequest walletRequest) throws InternalServerException {

        try {
            BetHistory betHistory = this.prepareBetHistoryEntity(walletTransaction, walletRequest);

            if (!walletRequest.getIsPreProcessBet()) {
                kafkaService.produceBetHistoryV3(betHistory, null, null, null, walletRequest.getOperatorUsername(), walletRequest.getVendorPlayerUsername());
            } else {
                kafkaService.producePreprocessingBetHistory(betHistory, walletRequest.getOperatorUsername(), walletRequest.getVendorPlayerUsername(), walletRequest.getFromVendorRate());
            }

        } catch (Exception e) {
            //TODO REVISIT AND CAPTURE THE FAILED INTERNAL PROCESS TO RERUN IT
            throw new InternalServerException(e.getMessage());

        }
        return walletRequest;

    }

    public WalletBetCreditDto prepareOperatorRequestData(WalletRequest walletRequest) {
        WalletBetCreditDto dto = new WalletBetCreditDto();
        BigDecimal amount = walletRequest.getTransferAmount();
        BigDecimal betAmount = walletRequest.getBetAmount();
        BigDecimal winAmount = walletRequest.getWinAmount();
        BigDecimal effectiveTurnover = walletRequest.getEffectiveTurnover();
        BigDecimal jackpotAmount = walletRequest.getJackpotAmount();
        BigDecimal winLoss = walletRequest.getWinLoss();
        BigDecimal vendorRate = walletRequest.getFromVendorRate();

        dto.setAmount(CurrencyConversionService.convertFromVendorRate(amount, vendorRate, true));
        dto.setBetAmount(CurrencyConversionService.convertFromVendorRate(betAmount, vendorRate, true));
        dto.setWinAmount(CurrencyConversionService.convertFromVendorRate(winAmount, vendorRate, true));
        dto.setWinLoss(CurrencyConversionService.convertFromVendorRate(winLoss, vendorRate, false)); // winloss can be negative, but still require conversion
        dto.setEffectiveTurnover(CurrencyConversionService.convertFromVendorRate(effectiveTurnover, vendorRate, true));
        dto.setJackpotAmount(CurrencyConversionService.convertFromVendorRate(jackpotAmount, vendorRate, true));

        dto.setTraceId(walletRequest.getTraceId());
        dto.setUsername(walletRequest.getOperatorUsername());
        dto.setTransactionId(walletRequest.getTransactionId());
        dto.setBetId((walletRequest.getBetId()));
        dto.setRoundId(walletRequest.getRoundId());
        dto.setGameCode(walletRequest.getGameCode());
        dto.setCurrency(walletRequest.getCurrencyCode());
        dto.setToken(walletRequest.getToken());
        dto.setBetTime(walletRequest.getVendorBetTime());
        dto.setSettledTime(walletRequest.getVendorSettleTime());
        dto.setTimestamp(walletRequest.getTimestamp());
        dto.setIsRefund((walletRequest.getBetStatus().equals(BetStatus.REFUNDED)) ? 1 : 0);

        return dto;
    }

    private BetHistory prepareBetHistoryEntity(WalletTransaction walletTransaction, WalletRequest walletRequest) {

        BetHistory betHistory = new BetHistory();
        betHistory.setId(walletTransaction.getBetId());
        betHistory.setExternalTransactionId(walletTransaction.getExternalTransactionId());
        betHistory.setVendorBetId(walletTransaction.getVendorBetId());
        betHistory.setRoundId(walletTransaction.getRoundId());
        betHistory.setVendorId(walletTransaction.getVendorId());
        betHistory.setCurrencyId(walletTransaction.getCurrencyId());
        betHistory.setGameSessionToken(walletTransaction.getToken());
        betHistory.setOperatorStatus(walletTransaction.getOperatorStatus());

        betHistory.setVendorLineId(walletRequest.getVendorLineId());
        betHistory.setVendorGameId(walletRequest.getVendorGameId());
        betHistory.setVendorPlayerId(walletRequest.getVendorPlayerId());
        betHistory.setAgentPlayerId(walletRequest.getAgentPlayerId());
        betHistory.setAgentId(walletRequest.getAgentId());
        betHistory.setGameCategoryId(walletRequest.getGameCategoryId());
        betHistory.setBetAmount(walletRequest.getBetAmount());
        betHistory.setWinAmount(walletRequest.getWinAmount());

        BigDecimal winLoss = walletRequest.getWinAmount().subtract(walletRequest.getBetAmount());

        betHistory.setWinLoss(winLoss);
        betHistory.setEffectiveTurnover(walletRequest.getEffectiveTurnover());
        betHistory.setJackpotAmount(walletRequest.getJackpotAmount());
        betHistory.setResultType(BetHistory.retrieveResultType(walletRequest.getWinAmount(), walletRequest.getJackpotAmount()));
        betHistory.setBetType(BetType.NORMAL_BET.code);
        betHistory.setIsFreespin(0);
        betHistory.setStatus((walletRequest.getBetStatus() == null) ? BetStatus.SETTLED.code : walletRequest.getBetStatus().code);
        betHistory.setVendorBetTime(walletRequest.getVendorBetTime());
        betHistory.setVendorSettleTime(walletRequest.getVendorSettleTime());
        betHistory.setResultTime(walletRequest.getTimestamp());

        return betHistory;
    }
}
