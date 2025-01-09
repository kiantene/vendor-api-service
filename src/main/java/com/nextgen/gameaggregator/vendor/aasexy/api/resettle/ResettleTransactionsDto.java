package com.nextgen.gameaggregator.vendor.aasexy.api.resettle;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.operator.wallet.adjustment.AdjustmentData;
import com.nextgen.gameaggregator.vendor.aasexy.dto.GameInfoDto;
import com.nextgen.gameaggregator.vendor.aasexy.service.VendorService;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResettleTransactionsDto implements AdjustmentData {
    @NotBlank
    @Size(max = 255)
    private String platformTxId;

    @NotBlank
    @Size(max = 50)
    private String userId;

    private String platform;

    private String refPlatformTxId;

    @NotBlank
    @Size(max = 255)
    private String settleType;

    private String gameType;

    @NotBlank
    @Size(max = 50)
    private String gameCode;

    private String gameName;

    @JsonProperty("betType")
    private String betTypes;

    @NotNull
    @Digits(integer = 20, fraction = 8)
    private BigDecimal betAmount;

    @NotNull
    @Digits(integer = 20, fraction = 8)
    private BigDecimal winAmount;

    private BigDecimal turnover;

    private String betTime;

    private String updateTime;

    private String txTime;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("roundId")
    private String roundId;

    @JsonIgnore
    private BigDecimal adjustmentAmount;

    private GameInfoDto gameInfo;

    @Override
    public String getVendorBetId() {
        return this.platformTxId;
    }

    @Override
    public String getExternalTransactionId() {
        return this.platformTxId;
    }

    @Override
    public String getGameId() {
        return this.gameCode;
    }

    @Override
    public BigDecimal getAdjustmentAmount() {
        return this.adjustmentAmount;
    }

    @Override
    public Long getTimestamp() {
        return VendorService.getTimeStamp(this.txTime);
    }
}
