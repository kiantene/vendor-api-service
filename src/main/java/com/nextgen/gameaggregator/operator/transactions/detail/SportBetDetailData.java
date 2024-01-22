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
    private String choice;
    private String odds;
    private BigDecimal stake;
    private BigDecimal winLoss;
    private String status;
    private List<SportParlayDetailData> parlayDetail;

    public SportBetDetailData(SportBetDetailVo sportBetDetailVo) {
        this.setBetNumber(sportBetDetailVo.getBetNumber());
        this.setVendorUsername(sportBetDetailVo.getVendorUsername());
        this.setReferenceNumber(sportBetDetailVo.getReferenceNumber());
        this.setTransactionTime(sportBetDetailVo.getTransactionTime());
        this.setChoice(sportBetDetailVo.getChoice());
        this.setOdds(sportBetDetailVo.getOdds());
        this.setStake(sportBetDetailVo.getStake());
        this.setWinLoss(sportBetDetailVo.getWinLoss());
        this.setStatus(sportBetDetailVo.getStatus());
        this.setParlayDetail(sportBetDetailVo.getParlayDetail());
    }
}
