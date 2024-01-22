package com.nextgen.gameaggregator.vendor.saba.api.betdetail;

import com.google.gson.annotations.SerializedName;
import com.nextgen.gameaggregator.operator.transactions.detail.SportBetDetailVo;
import com.nextgen.gameaggregator.operator.transactions.detail.SportParlayDetailData;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class BetDetailVo implements SportBetDetailVo {

    @SerializedName("error_code")
    private String errorCode;

    private String message;

    @SerializedName("Data")
    private DataDto data;

    @Override
    public String getBetNumber() {
        return null;
    }

    @Override
    public String getVendorUsername() {
        return null;
    }

    @Override
    public String getReferenceNumber() {
        return null;
    }

    @Override
    public Long getTransactionTime() {
        return null;
    }

    @Override
    public String getChoice() {
        return null;
    }

    @Override
    public String getOdds() {
        return null;
    }

    @Override
    public BigDecimal getStake() {
        return null;
    }

    @Override
    public BigDecimal getWinLoss() {
        return null;
    }

    @Override
    public String getStatus() {
        return null;
    }

    @Override
    public List<SportParlayDetailData> getParlayDetail() {
        return null;
    }
}
