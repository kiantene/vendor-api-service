package com.nextgen.gameaggregator.operator.transactions.detail;

import com.nextgen.gameaggregator.entity.custom.IBetDetailUrlInfo;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionDetailData {

    private String detailUrl;
    private BetDetail betDetail;
    @Data
    public static class BetDetail {
        String betId;
        String externalTransactionId;
        String roundId;
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
        this.betDetail = new BetDetail();
        this.betDetail.setBetId(iBetDetailUrlInfo.getBetId());
        this.betDetail.setExternalTransactionId(iBetDetailUrlInfo.getExternalTransactionId());
        this.betDetail.setRoundId(iBetDetailUrlInfo.getExternalRoundId());
        this.betDetail.setUsername(iBetDetailUrlInfo.getUsername());
        this.betDetail.setCurrencyCode(iBetDetailUrlInfo.getCurrencyCode());
        this.betDetail.setGameCode(iBetDetailUrlInfo.getGameCode());
        this.betDetail.setVendorCode(iBetDetailUrlInfo.getVendorCode());
        this.betDetail.setGameCategoryCode(iBetDetailUrlInfo.getGameCategoryCode());
        this.betDetail.setBetAmount(iBetDetailUrlInfo.getBetAmount());
        this.betDetail.setWinAmount(iBetDetailUrlInfo.getWinAmount());
        this.betDetail.setWinLoss(iBetDetailUrlInfo.getWinLoss());
        this.betDetail.setEffectiveTurnover(iBetDetailUrlInfo.getEffectiveTurnover());
        this.betDetail.setJackpotAmount(iBetDetailUrlInfo.getJackpotAmount());
        this.betDetail.setRefundAmount(iBetDetailUrlInfo.getRefundAmount());
        this.betDetail.setStatus(iBetDetailUrlInfo.getStatus());
        this.betDetail.setVendorBetTime(iBetDetailUrlInfo.getVendorBetTime());
        this.betDetail.setVendorSettleTime(iBetDetailUrlInfo.getVendorSettleTime());
        this.betDetail.setIsFreeSpin(iBetDetailUrlInfo.getIsFreeSpin());
    }
}
