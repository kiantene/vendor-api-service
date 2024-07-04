package com.nextgen.gameaggregator.vendor.epicwin.api.rollback;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RollbackDto implements RollbackData {
    @NotBlank
    @Size(min = 1, max = 20)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("OperatorId")
    private String operatorId;

    @NotBlank
    @Size(min = 1, max = 50)
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")
    @JsonProperty("RequestDateTime")
    private String requestDateTime;

    @NotBlank
    @Size(min = 1, max = 50)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("Signature")
    private String signature;

    @NotBlank
    @Size(min = 1, max = 50)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("PlayerId")
    private String playerId;

    @NotBlank
    @Size(min = 1, max = 5)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("Currency")
    private String currency;

    @NotNull
    @JsonProperty("RoundId")
    private Long roundId;

    @NotNull
    @JsonProperty("BetId")
    private Long betId;

    @NotBlank
    @Size(min = 1, max = 50)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("GameCode")
    private String gameCode;

    @JsonProperty("GameType")
    private String gameType;

    @NotNull
    @Digits(integer = 12, fraction = 4)
    @JsonProperty("BetAmount")
    private BigDecimal betAmount;

    @JsonProperty("ExchangeRate")
    private BigDecimal exchangeRate;

    @NotBlank
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")
    @JsonProperty("TranDateTime")
    private String tranDateTime;

    @JsonProperty("RollbackType")
    private Integer rollbackType;

    @JsonProperty("ProviderTimeZone")
    private String providerTimeZone;

    @NotBlank
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}[+-]\\d{2}:\\d{2}")
    @JsonProperty("ProviderTranDt")
    private String providerTranDt;

    @Override
    public String getRollbackId() {
        return String.valueOf(this.getBetId());
    }

    @Override
    public Long getVendorSettledTime() {
        return null;
    }
}
