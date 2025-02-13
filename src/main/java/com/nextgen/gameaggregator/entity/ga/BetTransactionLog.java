package com.nextgen.gameaggregator.entity.ga;

import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BetTransactionLog {
    private String betId;
    private String externalTransactionId;
    private String vendorBetId;
    private String roundId;
    private Integer vendorId;
    private BigDecimal betAmount;
    private BigDecimal winAmount;
    private BigDecimal jackpotAmount;
    private Integer resultType;
    private Integer isFreespin;
    private Integer status;
    private Long vendorBetTime;
    private Long vendorSettleTime;
    private Long createTime;
    public BetTransactionLog (BetInformation betInformation, BetResultData betResultData) {
        this.setBetId(betInformation.getBetId());
        this.setExternalTransactionId(betResultData.getExternalTransactionId());
        this.setVendorBetId(betResultData.getVendorBetId());
        this.setRoundId(betResultData.getRoundId());
        this.setVendorId(betInformation.getVendorId());
        this.setBetAmount(betResultData.getBetAmount());
        this.setWinAmount(betResultData.getWinAmount());
        this.setJackpotAmount(betResultData.getJackpotAmount());
        this.setResultType(betInformation.getResultType());
        this.setIsFreespin(betResultData.getIsFreespin());
        this.setStatus(betResultData.getBetStatus().code);
        this.setVendorBetTime(betResultData.getVendorBetTime());
        this.setVendorSettleTime(betResultData.getVendorSettleTime());
        this.setCreateTime(betInformation.getCreateTime());
    }

    public BetTransactionLog (BetInformation betInformation) {
        this.setBetId(betInformation.getBetId());
        this.setExternalTransactionId(betInformation.getExternalTransactionId());
        this.setVendorBetId(betInformation.getVendorBetId());
        this.setRoundId(betInformation.getRoundId());
        this.setVendorId(betInformation.getVendorId());
        this.setBetAmount(betInformation.getBetAmount());
        this.setWinAmount(betInformation.getWinAmount());
        this.setJackpotAmount(betInformation.getJackpotAmount());
        this.setResultType(betInformation.getResultType());
        this.setIsFreespin(betInformation.getIsFreespin());
        this.setStatus(betInformation.getStatus());
        this.setVendorBetTime(betInformation.getVendorBetTime());
        this.setVendorSettleTime(betInformation.getVendorSettleTime());
        this.setCreateTime(betInformation.getCreateTime());
    }

}
