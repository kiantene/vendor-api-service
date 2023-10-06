package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.enums.BetResultType;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.wallet.adjustment.AdjustmentData;
import com.nextgen.gameaggregator.operator.wallet.adjustment.WalletAdjustmentAction;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
public class WalletAdjustmentService {

    private final Integer operatorStatusSuccess = ResponseCodes.Status.SC_OK.code;
    @Autowired
    private BetAdjustmentLogService betAdjustmentLogService;
    @Autowired
    private KafkaService kafkaService;
    @Autowired
    private SettledBetService settledBetService;
    @Autowired
    private UnsettledBetService unsettledBetService;
    @Autowired
    private WalletAdjustmentAction walletAdjustmentAction;
    @Autowired
    private VendorService vendorService;

    public BigDecimal processAdjustment(String traceId, GameSession gameSession, AdjustmentData adjustmentData, HttpRequestLog httpRequestLog) throws BetAdjustmentIdempotentViolationException, BetNotFoundException, SettledBetNotFoundException, TransactionStillProcessingException, InvalidOperatorResponseException, InvalidAgentApiCredentialException, InsufficientBalanceException, VendorCurrencyNotSupportException {
        httpRequestLog.setRequestType(WalletAdjustmentAction.class.getSimpleName());
        httpRequestLog.setOperatorUsername(gameSession.getAgentPlayerUsername());
        httpRequestLog.setVendorId(gameSession.getVendorId());
        httpRequestLog.setRoundId(adjustmentData.getRoundId());
        httpRequestLog.setGameToken(gameSession.getToken());
        httpRequestLog.setBetStart(System.currentTimeMillis());
        httpRequestLog.setVendorUsername(gameSession.getVendorPlayerUsername());
        httpRequestLog.setVendorGameCode(gameSession.getVendorGameCode());
        WalletBalanceVo balanceVo = null;

        // check idempotent
        RawBetAdjustmentLog rawBetAdjustmentLog = this.betAdjustmentLogService.idempotentCheck(traceId, gameSession, adjustmentData);


        try {
            this.checkValidAdjustment(gameSession, adjustmentData);

            SettledBet settledBet = this.newAdjustmentSettledBet(traceId, adjustmentData, gameSession);

            VendorCurrency vendorCurrency = vendorService.getCurrencyConversionRate(gameSession, traceId);

            // Adjustment Request to Operator
            balanceVo = walletAdjustmentAction.call(traceId, gameSession.getAgentId(), gameSession, settledBet, httpRequestLog, vendorCurrency.getFromVendorRate(), vendorCurrency.getToVendorRate());

            // update operator status after receiving response from operator
            settledBet.setOperatorStatus(operatorStatusSuccess);
            settledBet.setBalance(balanceVo.getData().getBalance());

            // update operator status after receiving response from operator
            rawBetAdjustmentLog.setOperatorStatus(ResponseCodes.Status.SC_OK.code);
            rawBetAdjustmentLog.setBalance(balanceVo.getData().getBalance());
            betAdjustmentLogService.create(rawBetAdjustmentLog);

            // send settled bet to kafka
            BetHistory betHistory = new BetHistory(settledBet);
            kafkaService.produceBetHistory(betHistory, settledBet, vendorCurrency.getFromVendorRate());

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            rawBetAdjustmentLog.setOperatorStatus(invalidOperatorResponseException.getOperatorStatus());
            betAdjustmentLogService.create(rawBetAdjustmentLog);

            if (invalidOperatorResponseException.getOperatorStatus().equals(ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code)) {
                throw new InsufficientBalanceException();
            } else {
                throw invalidOperatorResponseException;
            }
        } catch (BetNotFoundException |
                 SettledBetNotFoundException betNotFoundException) {
            rawBetAdjustmentLog.setOperatorStatus(ResponseCodes.Status.SC_UNKNOWN_ERROR.code);
            betAdjustmentLogService.create(rawBetAdjustmentLog);
            
            throw betNotFoundException;
        }

        return balanceVo.getData().getBalance();
    }

    private SettledBet newAdjustmentSettledBet(String traceId, AdjustmentData adjustmentData, GameSession gameSession) {

        SettledBet settledBet = new SettledBet(adjustmentData, traceId, gameSession);
        settledBet.setWinLoss(adjustmentData.getAdjustmentAmount());
        settledBet.setOperatorStatus(ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code);
        settledBet.setResettleNum(1);
        settledBet.setJackpotAmount(BigDecimal.ZERO);
        settledBet.setIsFreespin(0);
        settledBet.setEffectiveTurnover(BigDecimal.ZERO);
        settledBet.setStatus(BetStatus.SETTLED.code);
        settledBet.setResultType(BetResultType.ADJUSTMENT.code);
        settledBet.setBalance(BigDecimal.ZERO);
        settledBet.setVendorBetTime(adjustmentData.getTimestamp());
        settledBet.setVendorSettleTime(adjustmentData.getTimestamp());

        return settledBet;
    }

    private void checkValidAdjustment(GameSession gameSession, AdjustmentData adjustmentData) throws BetNotFoundException, SettledBetNotFoundException {
        List<SettledBet> settledBetList = settledBetService.getByVendorPlayerIdAndRoundId(gameSession.getVendorPlayerId(), adjustmentData.getRoundId());
        if (settledBetList.isEmpty()) {
            List<UnsettledBet> unsettledBetList = unsettledBetService.getByRoundId(adjustmentData.getRoundId(), gameSession.getVendorGameId(), gameSession.getVendorPlayerId());
            if (unsettledBetList.isEmpty()) {
                throw new BetNotFoundException();
            } else {
                throw new SettledBetNotFoundException();
            }
        }
    }
}
