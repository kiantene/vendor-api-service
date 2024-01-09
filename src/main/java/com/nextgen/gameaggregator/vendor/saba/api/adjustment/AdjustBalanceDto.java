package com.nextgen.gameaggregator.vendor.saba.api.adjustment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.sport.adjustment.SportAdjustmentData;
import com.nextgen.gameaggregator.vendor.saba.dto.GeneralDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdjustBalanceDto extends GeneralDto implements SportAdjustmentData {

    private String time;
    private String userId;
    private String currency;
    private String txId;
    private String refId;
    private String operationId;
    private String betType;
    private String betTypeName;
    private String winLostDate;
    private AdjustBalanceInfoDto balanceInfo;

    @Override
    public String getVendorUsername() {
        return userId;
    }

    @Override
    public String getVendorBetId() {
        return txId;
    }

    @Override
    public String getRoundId() {
        return refId;
    }

    @Override
    public String getExternalTransactionId() {
        return refId;
    }

    @Override
    public BigDecimal getAmount() {
        return this.balanceInfo.getCreditAmount().subtract(this.balanceInfo.getDebitAmount());
    }

    @Override
    public Long getTimestamp() {
        return null;
    }
}
