package com.nextgen.gameaggregator.vendor.bglive.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.vendor.bglive.dto.CommonDto;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetDto extends CommonDto implements BetResultData {

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
        return null;
    }

    @Override
    public BigDecimal getBetAmount() {
        return currentOrder != null && currentOrder.getAmount() != null
                ? currentOrder.getAmount().abs()
                : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getWinAmount() {
        return null;
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
        return System.currentTimeMillis();
    }

    @Override
    public Long getResultTime() {
        return null;
    }

    @Override
    public Long getVendorSettleTime() {
        return null;
    }

    @Override
    public BigDecimal getJackpotAmount() {
        return null;
    }

    @Override
    public Integer getIsFreespin() {
        return 0;
    }

    @Override
    public BetStatus getBetStatus() {
        return BetStatus.UNSETTLED;
    }
}
