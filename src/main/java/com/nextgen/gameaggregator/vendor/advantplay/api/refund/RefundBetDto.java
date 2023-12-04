package com.nextgen.gameaggregator.vendor.advantplay.api.refund;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.vendor.advantplay.dto.BetSettleRefundDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class RefundBetDto extends BetSettleRefundDto implements RollbackData {
    private BigDecimal totalStake;

    @Override
    public String getRollbackId() {
        return this.getGameRoundId();
    }

    @Override
    public Long getVendorSettledTime() {
        return null;
    }

}
