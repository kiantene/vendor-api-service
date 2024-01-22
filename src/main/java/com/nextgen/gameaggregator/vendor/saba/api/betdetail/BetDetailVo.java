package com.nextgen.gameaggregator.vendor.saba.api.betdetail;

import com.google.gson.annotations.SerializedName;
import com.nextgen.gameaggregator.operator.constant.SportBetStatus;
import com.nextgen.gameaggregator.operator.transactions.detail.SportBetDetailVo;
import com.nextgen.gameaggregator.operator.transactions.detail.SportParlayDetailData;
import com.nextgen.gameaggregator.vendor.saba.service.VendorService;
import lombok.Data;

import java.math.BigDecimal;
import java.util.LinkedList;
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
        return this.getData().getBetDetails().get(0).getTransId();
    }

    @Override
    public String getVendorUsername() {
        return this.getData().getBetDetails().get(0).getVendorMemberId();
    }

    @Override
    public String getReferenceNumber() {
        return this.getData().getBetDetails().get(0).getTransId();
    }

    @Override
    public Long getTransactionTime() {
        return VendorService.convertToUnixTimestamp(this.getData().getBetDetails().get(0).getSettlementTime());
    }

    @Override
    public String getChoice() {
        return null;
    }

    @Override
    public String getOdds() {
        return this.getData().getBetDetails().get(0).getOdds();
    }

    @Override
    public BigDecimal getStake() {
        return new BigDecimal(this.getData().getBetDetails().get(0).getStake());
    }

    @Override
    public BigDecimal getWinLoss() {
        return new BigDecimal(this.getData().getBetDetails().get(0).getWinlostAmount());
    }

    @Override
    public String getStatus() {
        return SportBetStatus.BetStatus.WIN.value;
    }

    @Override
    public List<SportParlayDetailData> getParlayDetail() {
        return new LinkedList<>();
    }
}
