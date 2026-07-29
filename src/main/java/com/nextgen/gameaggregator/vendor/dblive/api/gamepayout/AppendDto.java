package com.nextgen.gameaggregator.vendor.dblive.api.gamepayout;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.wallet.adjustment.AdjustmentData;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppendDto implements AdjustmentData {
    @NotNull
    private BigDecimal payoutAmount;
    @NotNull
    private Long payoutTime;
    @NotBlank
    @Size(max = 255)
    private String gameTypeId;
    @NotNull
    private Long transferNo;
    @NotBlank
    @Size(max = 255)
    private String loginName;
    @NotBlank
    @Size(max = 255)
    private String playerId;
    @NotBlank
    @Size(max = 255)
    private String transferType;
    @NotNull
    private Map<String, Double> betPayoutMap;
    @NotBlank
    private String currency;
    @NotBlank
    @Size(max = 255)
    private String roundNo;

    @Override
    public String getVendorBetId() {
        return String.valueOf(this.transferNo);
    }

    @Override
    public String getRoundId() {
        return this.roundNo;
    }

    @Override
    public String getExternalTransactionId() {
        return String.valueOf(this.transferNo);
    }

    @Override
    public String getGameId() {
        return this.gameTypeId;
    }

    @Override
    public BigDecimal getAdjustmentAmount() {
        return this.payoutAmount;
    }

    @Override
    public Long getTimestamp() {
        return this.payoutTime;
    }
}
