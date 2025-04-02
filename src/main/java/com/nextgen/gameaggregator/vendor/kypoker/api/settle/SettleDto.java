package com.nextgen.gameaggregator.vendor.kypoker.api.settle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.*;
import jnr.ffi.annotations.In;
import lombok.Data;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class SettleDto implements BetResultData {

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
    @Size(min = 1, max = 36)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_COLON_REGEX)
    @JsonProperty("orderId")
    private String orderId;

    @NotBlank
    @Size(min = 1, max = 36)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_COLON_REGEX)
    @JsonProperty("gameNo")
    private String gameNo;

    @NotNull
    @JsonProperty("kindID")
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

    @NotBlank
    @Size(min = 1, max = 36)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_COLON_REGEX)
    @JsonProperty("gameId")
    private String gameId;

    @NotNull
    @JsonProperty("roomMode")
    @Digits(integer = 1, fraction = 0)
    private Integer roomMode;

    @NotNull
    @JsonProperty("betCount")
    @Digits(integer = 35, fraction = 0)
    private Integer betCount;

    @NotNull
    @JsonProperty("totalBet")
    @Digits(integer = 20, fraction = 8)
    private BigDecimal totalBet;

    @NotNull
    @JsonProperty("validBet")
    @Digits(integer = 20, fraction = 8)
    private BigDecimal validBet;

    @NotNull
    @JsonProperty("totalWithdraw")
    @Digits(integer = 20, fraction = 8)
    private BigDecimal totalWithdraw;

    @NotNull
    @JsonProperty("revenue")
    @Digits(integer = 20, fraction = 8)
    private BigDecimal revenue;

    private Long timeStamp;

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
        return String.valueOf(this.kindId);
    }

    @Override
    public BigDecimal getBetAmount() {
        return this.validBet;
    }

    @Override
    public BigDecimal getWinAmount() {
        return this.totalWithdraw.compareTo(BigDecimal.ZERO) >= 0
                ? this.totalWithdraw
                : this.validBet.add(this.totalWithdraw);
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
        return 0L;
    }

    @Override
    public Long getResultTime() {
        return this.timeStamp;
    }

    @Override
    public Long getVendorSettleTime() {
        return this.timeStamp;
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
