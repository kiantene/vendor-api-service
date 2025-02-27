package com.nextgen.gameaggregator.vendor.bglive.api.settlement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.vendor.bglive.dto.CommonDto;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper = true)
public class SettleDto extends CommonDto implements BetResultData {

    @JsonProperty("params")
    private ParamsDto paramsDto;

    @JsonProperty("orders")
    private OrdersDto currentOrder;

    @Override
    public String getExternalTransactionId() {
        return currentOrder != null ? currentOrder.getOrderId() : null;

    }

    @Override
    public String getVendorBetId() {
        return getExternalTransactionId();
    }

    @Override
    public String getRoundId() {
        return getExternalTransactionId();
    }

    @Override
    public String getGameId() {
        return currentOrder != null ? currentOrder.getGameId() : null;
    }

    @Override
    public BigDecimal getBetAmount() {
        return currentOrder != null && currentOrder.getOrderAmount() != null
                ? currentOrder.getAmount().abs()
                : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getWinAmount() {
        return currentOrder != null ? currentOrder.getAmount() : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getWinLoss() {
        return null;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return null;
    }

    @Override
    public Long getVendorBetTime() {
        return null;
    }

    @Override
    public Long getResultTime() {
        return null;
    }

    @Override
    public Long getVendorSettleTime() {
        return System.currentTimeMillis();
    }

    @Override
    public BigDecimal getJackpotAmount() {
        return BigDecimal.ZERO;
    }

    @Override
    public Integer getIsFreespin() {
        return 0;
    }

    @Override
    public BetStatus getBetStatus() {
        return BetStatus.SETTLED;
    }
}
