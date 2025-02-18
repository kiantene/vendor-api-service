package com.nextgen.gameaggregator.vendor.bglive.api.settlement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SettleDto implements BetResultData {

    @NotBlank
    @Size(max = 255)
    @JsonProperty("id")
    private String id;

    @JsonProperty("params")
    private ParamsDto params;

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
        return currentOrder != null && currentOrder.getAmount() != null
                ? currentOrder.getAmount().abs()
                : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getWinAmount() {
        return currentOrder != null ? currentOrder.getAmount() : null;
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
        return null;
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
