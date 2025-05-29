package com.nextgen.gameaggregator.vendor.kypoker.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.core.RequestIdempotency;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class BetDto implements BetResultData, RequestIdempotency {

    @NotBlank
    @Size(min = 1, max = 36)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_COLON_REGEX)
    @JsonProperty("s")
    private String s;

    @NotBlank
    @Size(min = 1, max = 36)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_COLON_REGEX)
    @JsonProperty("account")
    private String account;

    @NotBlank
    @Size(min = 1, max = 50)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_COLON_REGEX)
    @JsonProperty("orderId")
    private String orderId;

    @NotBlank
    @Size(min = 1, max = 36)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_COLON_REGEX)
    @JsonProperty("gameNo")
    private String gameNo;

    @NotBlank
    @Size(min = 1, max = 36)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_COLON_REGEX)
    @JsonProperty("gameId")
    private String gameId;

    @NotNull
    @JsonProperty("kindId")
    @Digits(integer = 5, fraction = 0)
    private Integer kindId;

    @NotNull
    @JsonProperty("money")
    @Digits(integer = 20, fraction = 8)
    private BigDecimal money;

    @NotBlank
    @Size(min = 1, max = 36)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_COLON_REGEX)
    @JsonProperty("currency")
    private String currency;

    private Long timeStamp;

    @NotNull
    @JsonProperty("roomMode")
    @Digits(integer = 1, fraction = 0)
    private Integer roomMode;

    @Override
    public String getExternalTransactionId() {
        return this.orderId;
    }

    @Override
    public String getVendorBetId() {
        return this.orderId;
    }

    @Override
    public String getRoundId() {
        return this.gameNo;
    }

    @Override
    public String getGameId() {
        return String.valueOf(this.gameId);
    }

    @Override
    public BigDecimal getBetAmount() {
        return this.money;
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
        return this.timeStamp;
    }

    @Override
    public Long getResultTime() {
        return Long.valueOf(0);
    }

    @Override
    public Long getVendorSettleTime() {
        return Long.valueOf(0);
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

    @Override
    public String getTransactionId() {
        return getExternalTransactionId();
    }

    @Override
    public String getVendorPlayerUsername() {
        return getS();
    }
}
