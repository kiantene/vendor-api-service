package com.nextgen.gameaggregator.vendor.iloveu.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.iloveu.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.iloveu.service.VendorService;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetTransactionDto implements BetResultData {

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("sn")
    public String sn;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("id")
    public String id;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("method")
    public String method;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("loginId")
    public String loginId;

    @NotBlank(message = ResponseCodes.INVALID_SIGNATURE)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.INVALID_SIGNATURE)
    @JsonProperty("signature")
    public String signature;

    @NotNull
    @Range(min = 0, max = 1000000000000000L)
    @Digits(integer = 18, fraction = 4)
    @JsonProperty("totalBet")
    public BigDecimal totalBet;

    @JsonProperty("betDetail")
    public String betDetail;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("orderCode")
    public String orderCode;

    @NotBlank
    @Pattern(regexp = "^[a-zA-Z0-9:._ -]+$")
    @JsonProperty("actionDate")
    public String actionDate;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("gameName")
    public String gameName;

    @NotNull
    @Range(min = 0, max = 1000000000000000L)
    @Digits(integer = 18, fraction = 4)
    @JsonProperty("validBet")
    public BigDecimal validBet;

    @NotNull
    @Range(min = 0)
    @JsonProperty("gameStatus")
    public Integer gameStatus;

    @JsonProperty("playQueryCode")
    public String playQueryCode;

    @Override
    public String getExternalTransactionId() {
        return this.getId();
    }

    @Override
    public String getVendorBetId() {
        return this.getOrderCode();
    }

    @Override
    public String getRoundId() {
        return this.getOrderCode();
    }

    @Override
    public String getGameId() {
        return this.getGameName().toLowerCase();
    }

    @Override
    public BigDecimal getBetAmount() {
        return this.getValidBet();
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
        return VendorService.dateTimeConvert(this.actionDate);
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
        return null;
    }

    @Override
    public BetStatus getBetStatus() {
        return BetStatus.UNSETTLED;
    }
}