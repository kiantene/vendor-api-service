package com.nextgen.gameaggregator.vendor.epicwin.api.bonus;

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
public class BonusDto implements BetResultData {
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

    @NotBlank
    @Size(min = 1, max = 25)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("TranId")
    private String tranId;

    @NotBlank
    @Size(min = 1, max = 50)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("BonusId")
    private String bonusId;

    @JsonProperty("BonusName")
    private String bonusName;

    @NotNull
    @Digits(integer = 12, fraction = 4)
    @JsonProperty("Payout")
    private BigDecimal payout;

    @JsonProperty("ExchangeRate")
    private BigDecimal exchangeRate;

    @NotBlank
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")
    @JsonProperty("TranDateTime")
    private String tranDateTime;

    @JsonProperty("Result")
    private String result;

    @JsonProperty("ProviderTimeZone")
    private String providerTimeZone;

    @NotBlank
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}[+-]\\d{2}:\\d{2}")
    @JsonProperty("ProviderTranDt")
    private String providerTranDt;

    @Override
    public String getExternalTransactionId() {
        return String.valueOf(this.tranId);
    }

    @Override
    public String getVendorBetId() {
        return String.valueOf(this.tranId);
    }

    @Override
    public String getRoundId() {
        return String.valueOf(this.tranId);
    }

    @Override
    public String getGameId() {
        return null;
    }

    @Override
    public BigDecimal getBetAmount() {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getWinAmount() {
        return this.payout;
    }

    @Override
    public BigDecimal getWinLoss() {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return BigDecimal.ZERO;
    }

    @Override
    public Long getVendorBetTime() {
        return VendorService.convertDateTimeStringToTimestamp(this.getTranDateTime(), Formats.DATE_FORMAT, ZoneId.of(Formats.TIME_ZONE));
    }

    @Override
    public Long getResultTime() {
        return VendorService.convertDateTimeStringToTimestamp(this.getTranDateTime(), Formats.DATE_FORMAT, ZoneId.of(Formats.TIME_ZONE));
    }

    @Override
    public Long getVendorSettleTime() {
        return VendorService.convertDateTimeStringToTimestamp(this.getTranDateTime(), Formats.DATE_FORMAT, ZoneId.of(Formats.TIME_ZONE));
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
