package com.nextgen.gameaggregator.operator.transactions.detail;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SportBetDetailData {
    private String betNumber;
    private String vendorUsername;
    private String referenceNumber;
    private Long transactionTime;
    private List<MatchDetailData> matchDetail;
    private String odds;
    private BigDecimal stake;
    private BigDecimal winLoss;
    private String status;
    private Boolean isCashout;
    private BigDecimal cashoutAmount;

    public SportBetDetailData(SportBetDetailVo sportBetDetailVo) {
        this.setBetNumber(sportBetDetailVo.getBetNumber());
        this.setVendorUsername(sportBetDetailVo.getVendorUsername());
        this.setReferenceNumber(sportBetDetailVo.getReferenceNumber());
        this.setTransactionTime(sportBetDetailVo.getTransactionTime());
        this.setMatchDetail(sportBetDetailVo.getMatchDetail());
        this.setOdds(sportBetDetailVo.getOdds());
        this.setStake(sportBetDetailVo.getStake());
        this.setWinLoss(sportBetDetailVo.getWinLoss());
        this.setStatus(sportBetDetailVo.getStatus());
        this.setIsCashout(sportBetDetailVo.getIsCashout());
        this.setCashoutAmount(sportBetDetailVo.getCashoutAmount());
    }
}
