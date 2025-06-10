package com.nextgen.gameaggregator.vendor.tbp.api.cancel;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.*;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CancelDto implements RollbackData {

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("UserName")
    private String username;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("Password")
    private String password;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("PlayerId")
    private String playerId;

    @NotBlank
    @Size(max = 255)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("RoundIdBI")
    private String roundIdBI;

    @NotBlank
    @Size(max = 255)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("TransferId")
    private String transferId;

    @JsonProperty("CasinoTransferId")
    private String casinoTransferId;

    @NotNull
    @JsonProperty("GameId")
    private Long gameIdv;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("GameNumber")
    private String gameNumber;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("SessionId")
    private String sessionId;

    @NotNull
    @Digits(integer = 20, fraction = 8)
    @JsonProperty("Amount")
    private BigDecimal amount;

    @NotBlank
    @JsonProperty("Currency")
    private String currency;

    @JsonProperty("Reason")
    private String reason;

    @JsonProperty("PlatformType")
    private String platformType;

    @Override
    public String getRollbackId() {
        return this.transferId;
    }

    @Override
    public Long getVendorSettledTime() {
        return System.currentTimeMillis();
    }

    @Override
    public String getRoundId() {
        return this.roundIdBI;
    }
}