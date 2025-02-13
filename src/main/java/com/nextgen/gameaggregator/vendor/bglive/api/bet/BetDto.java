package com.nextgen.gameaggregator.vendor.bglive.api.bet;

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
public class BetDto implements BetResultData {
    @NotBlank
    @Size(max = 255)
    @JsonProperty("id")
    private String id;

    @JsonProperty("params")
    private ParamsDto params;

    @Override
    public String getExternalTransactionId() {
        return params.getOrders() != null && !params.getOrders().isEmpty() ? params.getOrders().get(0).getOrderId() : null;
    }

    @Override
    public String getVendorBetId() {
        return getExternalTransactionId();
    }

    @Override
    public String getRoundId() {
        return params.getOrders() != null && !params.getOrders().isEmpty() ? params.getOrders().get(0).getIssueId() : null;
    }

    @Override
    public String getGameId() {
        return params.getOrders() != null && !params.getOrders().isEmpty() ? params.getOrders().get(0).getGameId() : null;
    }

    @Override
    public BigDecimal getBetAmount() {
        return params.getOrders() != null && !params.getOrders().isEmpty() ? params.getOrders().get(0).getAmount() : BigDecimal.ZERO;
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
