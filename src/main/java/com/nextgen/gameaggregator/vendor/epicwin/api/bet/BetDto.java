package com.nextgen.gameaggregator.vendor.epicwin.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.epicwin.constant.Formats;
import com.nextgen.gameaggregator.vendor.epicwin.service.VendorService;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.ZoneId;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetDto implements BetResultData {
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

    @NotBlank
    @Size(min = 1, max = 50)
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

    @NotBlank
    @Size(min = 1, max = 500)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("AuthToken")
    private String authToken;

    @JsonProperty("ProviderTimeZone")
    private String providerTimeZone;

    @NotBlank
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}[+-]\\d{2}:\\d{2}")
    @JsonProperty("ProviderTranDt")
    private String providerTranDt;

    @Override
    public String getExternalTransactionId() {
        return String.valueOf(this.betId);
    }

    @Override
    public String getVendorBetId() {
        return String.valueOf(this.betId);
    }

    @Override
    public String getRoundId() {
        return String.valueOf(this.roundId);
    }

    @Override
    public String getGameId() {
        return this.gameCode;
    }

    @Override
    public BigDecimal getBetAmount() {
        return this.betAmount;
    }

    @Override
    public BigDecimal getWinAmount() {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getWinLoss() {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return this.betAmount;
    }

    @Override
    public Long getVendorBetTime() {
        return VendorService.convertDateTimeStringToTimestamp(this.getTranDateTime(), Formats.DATE_FORMAT, ZoneId.of(Formats.TIME_ZONE));
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
        return BigDecimal.ZERO;
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
