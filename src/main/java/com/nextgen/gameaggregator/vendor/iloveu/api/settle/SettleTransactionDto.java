package com.nextgen.gameaggregator.vendor.iloveu.api.settle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.iloveu.constant.GameType;
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
public class SettleTransactionDto implements BetResultData {

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("SN")
    public String sn;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("ID")
    public String id;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("Method")
    public String method;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("LoginId")
    public String loginId;

    @NotBlank(message = ResponseCodes.INVALID_SIGNATURE)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.INVALID_SIGNATURE)
    @JsonProperty("Signature")
    public String signature;

    @NotNull
    @Range(min = 0, max = 1000000000000000L)
    @Digits(integer = 18, fraction = 4)
    @JsonProperty("TotalWin")
    public BigDecimal totalWin;

    @JsonProperty("WinDetail")
    public String winDetail;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("OrderCode")
    public String orderCode;

    @NotBlank
    @Pattern(regexp = "^[a-zA-Z0-9:._ -]+$")
    @JsonProperty("ActionDate")
    public String actionDate;

    @NotBlank
    @JsonProperty("GameName")
    public String gameName;

    @NotNull
    @Range(min = 0, max = 1000000000000000L)
    @Digits(integer = 18, fraction = 4)
    @JsonProperty("ValidCommission")
    public BigDecimal validCommission;

    @NotNull
    @Range(min = 0, max = 1000000000000000L)
    @Digits(integer = 18, fraction = 4)
    @JsonProperty("ValidWin")
    public BigDecimal validWin;

    @NotNull
    @Range(min = 0, max = 1000000000000000L)
    @Digits(integer = 18, fraction = 4)
    @JsonProperty("MJPWin")
    public BigDecimal mjpWin;

    @NotNull
    @Range(min = 0, max = 1000000000000000L)
    @Digits(integer = 18, fraction = 4)
    @JsonProperty("MJPComm")
    public BigDecimal mjpComm;

    @NotNull
    @Range(min = -1000000000000000L, max = 1000000000000000L)
    @Digits(integer = 18, fraction = 4)
    @JsonProperty("Profit")
    public BigDecimal profit;

    @JsonProperty("Bet")
    public SettleBetDto bet;

    @NotNull
    @Range(min = 3, max = 4)
    @JsonProperty("Mode")
    public Integer mode;

    @NotNull
    @Range(min = 0)
    @JsonProperty("GameStatus")
    public Integer gameStatus;

    @JsonProperty("PlayQueryCode")
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

        BigDecimal betAmount = null;
        if (this.getMode().equals(GameType.BETNSETTLE.code)) {
            betAmount = this.getBet().getValidBet();
        }
        return betAmount;
    }

    @Override
    public BigDecimal getWinAmount() {
        BigDecimal winAmount = this.getValidWin();
        if (this.getGameStatus() >= 240 && this.getGameStatus() <= 250) {
            //jackpot condition
            winAmount = BigDecimal.ZERO;
        }
        return winAmount;
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
        Long vendorBetTime = null;
        if (this.getMode().equals(GameType.BETNSETTLE.code)) {
            vendorBetTime = VendorService.dateTimeConvert(this.getActionDate());
        }
        return vendorBetTime;
    }

    @Override
    public Long getResultTime() {
        return VendorService.dateTimeConvert(this.getActionDate());
    }

    @Override
    public Long getVendorSettleTime() {
        return VendorService.dateTimeConvert(this.getActionDate());
    }

    @Override
    public BigDecimal getJackpotAmount() {
        BigDecimal jackpotAmount = BigDecimal.ZERO;
        if (this.getGameStatus() >= 240 && this.getGameStatus() <= 250) {
            //jackpot condition
            jackpotAmount = (this.getMjpWin().compareTo(BigDecimal.ZERO) > 0) ? this.getMjpWin() : this.getValidWin();
        }
        return jackpotAmount;
    }

    @Override
    public Integer getIsFreespin() {
        Integer freespin = 0;
        if (this.getGameStatus().equals(1)) {
            //freespin condition
            freespin = 1;
        }
        return freespin;
    }

    @Override
    public BetStatus getBetStatus() {
        return BetStatus.SETTLED;
    }
}