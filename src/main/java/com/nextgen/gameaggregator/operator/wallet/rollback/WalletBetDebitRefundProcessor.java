package com.nextgen.gameaggregator.operator.wallet.rollback;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.BetHistory;
import com.nextgen.gameaggregator.entity.ga.WalletTransaction;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.enums.BetType;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.KafkaService;
import com.nextgen.gameaggregator.service.WalletTransactionService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class WalletBetDebitRefundProcessor {

    private final WalletRequestService walletRequestService;
    private final WalletTransactionService walletTransactionService;
    private final KafkaService kafkaService;

    public WalletBetDebitRefundProcessor(WalletRequestService walletRequestService,
                                         WalletTransactionService walletTransactionService,
                                         KafkaService kafkaService) {

        this.walletRequestService = walletRequestService;
        this.walletTransactionService = walletTransactionService;
        this.kafkaService = kafkaService;
    }

    public WalletRequest process(WalletRequest walletRequest) throws InternalServerException, BetNotAllowedException, InsufficientBalanceException, InvalidOperatorResponseException, BetNotFoundException {

        walletRequestService.initialise(walletRequest);

        Integer vendorId = walletRequest.getVendorId();
        String externalTransactionId = walletRequest.getExternalTransactionId();

        WalletTransaction walletTransaction = walletTransactionService.getByVendorIdAndExternalTransactionId(vendorId, externalTransactionId);

        if (walletTransaction == null) {
            throw new BetNotFoundException(externalTransactionId + " not found in wallet transaction");
        }

        this.dataMapper(walletRequest, walletTransaction);

        WalletRollbackDto dto = this.prepareOperatorRequestData(walletRequest);

        new WalletRefundAction().callToOperator(walletRequest, dto);

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
        betHistory.setStatus(BetStatus.REFUNDED.code);
        betHistory.setVendorBetTime(walletRequest.getVendorBetTime());
        betHistory.setVendorSettleTime(walletRequest.getVendorSettleTime());
        betHistory.setResultTime(walletRequest.getTimestamp());

        return betHistory;
    }

    public WalletRollbackDto prepareOperatorRequestData(WalletRequest walletRequest) {
        WalletRollbackDto dto = new WalletRollbackDto();

        dto.setTraceId(walletRequest.getTraceId());
        dto.setTransactionId(walletRequest.getTransactionId());
        dto.setBetId(walletRequest.getBetId());
        dto.setExternalTransactionId(walletRequest.getExternalTransactionId());
        dto.setRoundId(walletRequest.getRoundId());
        dto.setGameCode(walletRequest.getGameCode());
        dto.setUsername(walletRequest.getOperatorUsername());
        dto.setCurrency(walletRequest.getCurrencyCode());
        dto.setTimestamp(walletRequest.getTimestamp());

        return dto;
    }

    private void dataMapper(WalletRequest walletRequest, WalletTransaction walletTransaction) {
        walletRequest.setToken(walletTransaction.getToken());
        walletRequest.setVendorGameCode(walletTransaction.getVendorGameCode());
        walletRequest.setCurrencyId(walletTransaction.getCurrencyId());
        walletRequest.setVendorBetId(walletTransaction.getVendorBetId());
        walletRequest.setRoundId(walletTransaction.getRoundId());
        walletRequest.setResultType(ResultType.BET_LOSE.code);
        walletRequest.setBetType(BetStatus.REFUNDED.code);
    }
}
