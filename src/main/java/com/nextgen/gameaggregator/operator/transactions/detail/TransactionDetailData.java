package com.nextgen.gameaggregator.operator.transactions.detail;

import com.nextgen.gameaggregator.entity.custom.IBetDetailUrlInfo;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionDetailData {

    private String detailUrl;
    private Betdetail betdetail;
    @Data
    public static class Betdetail {
        String transactionId;
        String externalTransactionId;
        String externalRoundId;
        String username;
        String currencyCode;
        String gameCode;
        String vendorCode;
        String gameCategoryCode;
        BigDecimal betAmount;
        BigDecimal winAmount;
        BigDecimal winLoss;
        BigDecimal effectiveTurnover;
        BigDecimal jackpotAmount;
        BigDecimal refundAmount;
        Integer status;
        String isFreeSpin;
        Long vendorBetTime;
        Long vendorSettleTime;

    }
    public void setBetDetail(IBetDetailUrlInfo iBetDetailUrlInfo){
        this.betdetail = new Betdetail();
        this.betdetail.setTransactionId(iBetDetailUrlInfo.getTransactionId());
        this.betdetail.setExternalTransactionId(iBetDetailUrlInfo.getExternalTransactionId());
        this.betdetail.setExternalRoundId(iBetDetailUrlInfo.getExternalRoundId());
        this.betdetail.setUsername(iBetDetailUrlInfo.getUsername());
        this.betdetail.setCurrencyCode(iBetDetailUrlInfo.getCurrencyCode());
        this.betdetail.setGameCode(iBetDetailUrlInfo.getGameCode());
        this.betdetail.setVendorCode(iBetDetailUrlInfo.getVendorCode());
        this.betdetail.setGameCategoryCode(iBetDetailUrlInfo.getGameCategoryCode());
        this.betdetail.setBetAmount(iBetDetailUrlInfo.getBetAmount());
        this.betdetail.setWinAmount(iBetDetailUrlInfo.getWinAmount());
        this.betdetail.setWinLoss(iBetDetailUrlInfo.getWinLoss());
        this.betdetail.setEffectiveTurnover(iBetDetailUrlInfo.getEffectiveTurnover());
        this.betdetail.setJackpotAmount(iBetDetailUrlInfo.getJackpotAmount());
        this.betdetail.setRefundAmount(iBetDetailUrlInfo.getRefundAmount());
        this.betdetail.setStatus(iBetDetailUrlInfo.getStatus());
        this.betdetail.setVendorBetTime(iBetDetailUrlInfo.getVendorBetTime());
        this.betdetail.setVendorSettleTime(iBetDetailUrlInfo.getVendorSettleTime());
        this.betdetail.setIsFreeSpin(iBetDetailUrlInfo.getIsFreeSpin());
    }
}
