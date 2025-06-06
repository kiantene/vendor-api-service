package com.nextgen.gameaggregator.vendor.marblex.api.resettle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.operator.sport.adjustment.SportAdjustmentData;
import com.nextgen.gameaggregator.util.DateTimeConverter;
import com.nextgen.gameaggregator.vendor.marblex.constant.Formats;
import com.nextgen.gameaggregator.vendor.marblex.dto.CommonDto;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResettleDto extends CommonDto implements SportAdjustmentData {

    @NotBlank
    @Size(max = 255)
    @JsonProperty("RoundID")
    private String roundId;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("GameCode")
    private String gameCode;

    @NotNull
    @Digits(integer = 20, fraction = 8)
    @JsonProperty("Winloss")
    private BigDecimal winLoss;

    @NotNull
    @Digits(integer = 20, fraction = 8)
    @JsonProperty("ReturnToWallet")
    private BigDecimal returnToWallet;

    @NotNull
    @Digits(integer = 20, fraction = 8)
    @JsonProperty("AdjustmentAmount")
    private BigDecimal adjustmentAmount;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("JanusTransactionID")
    private String janusTransactionId;
    
    @NotBlank
    @Pattern(regexp = Formats.TIME_REGEX)
    @JsonProperty("TransactionTime")
    private String transactionTime;

    @Override
    public String getVendorGameCode() {
        return this.gameCode;
    }

    @Override
    public String getVendorUsername() {
        return this.getPlayerId();
    }

    @Override
    public String getVendorBetId() {
        return this.janusTransactionId;
    }

    @Override
    public String getRoundId() {
        return this.roundId;
    }

    @Override
    public String getExternalTransactionId() {
        return this.janusTransactionId;
    }

    @Override
    public BigDecimal getAmount() {
        return this.adjustmentAmount;
    }

    @Override
    public Long getTimestamp() {
        return DateTimeConverter.convertToTimestamp(this.transactionTime, DateTimeConverter.ISO_8601);
    }
}
