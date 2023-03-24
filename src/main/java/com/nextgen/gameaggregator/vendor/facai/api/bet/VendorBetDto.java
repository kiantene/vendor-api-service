package com.nextgen.gameaggregator.vendor.facai.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.facai.constant.ResponseCodes;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.*;
import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VendorBetDto {

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 1, max = 24)
    @JsonProperty("RecordID")
    public String recordID;

    @NotNull(message = ResponseCodes.PARAM_CONTAIN_ERROR)
    @JsonProperty("BankID")
    public Long bankID;

    @NotBlank(message = ResponseCodes.PLAYER_NOT_FOUND)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.PLAYER_NOT_FOUND)
    @Size(min = 2, max = 30, message = ResponseCodes.PLAYER_NOT_FOUND)
    @JsonProperty("MemberAccount")
    public String memberAccount;

    @NotBlank(message = ResponseCodes.CURRENCY_MISSING)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.CURRENCY_MISSING)
    @Size(min = 3, max = 3, message = ResponseCodes.CURRENCY_MISSING)
    @JsonProperty("Currency")
    public String currency;

    @PositiveOrZero(message = ResponseCodes.PARAM_CONTAIN_ERROR)
    @NotNull(message = ResponseCodes.PARAM_CONTAIN_ERROR)
    @JsonProperty("GameID")
    public Integer gameID;

    @PositiveOrZero(message = ResponseCodes.GAME_TYPE_MISSING)
    @NotNull(message = ResponseCodes.GAME_TYPE_MISSING)
    @JsonProperty("GameType")
    public Integer gameType;

    @NotNull(message = ResponseCodes.PARAM_CONTAIN_ERROR)
    @JsonProperty("isBuyFeature")
    public Boolean isBuyFeature;

    @NotNull(message = ResponseCodes.PARAM_CONTAIN_ERROR)
    @Range(min = 0, message = ResponseCodes.PARAM_CONTAIN_ERROR)
    @Digits(integer = 12, fraction = 4, message = ResponseCodes.PARAM_CONTAIN_ERROR)
    @JsonProperty("Bet")
    public BigDecimal bet;

    @NotNull(message = ResponseCodes.PARAM_CONTAIN_ERROR)
    @Range(min = 0, message = ResponseCodes.PARAM_CONTAIN_ERROR)
    @Digits(integer = 12, fraction = 4, message = ResponseCodes.PARAM_CONTAIN_ERROR)
    @JsonProperty("Win")
    public BigDecimal win;

    @NotNull(message = ResponseCodes.PARAM_CONTAIN_ERROR)
    @Range(min = 0, message = ResponseCodes.PARAM_CONTAIN_ERROR)
    @Digits(integer = 12, fraction = 4, message = ResponseCodes.PARAM_CONTAIN_ERROR)
    @JsonProperty("JPBet")
    public BigDecimal jpBet;

    @NotNull(message = ResponseCodes.PARAM_CONTAIN_ERROR)
    @Range(min = 0, message = ResponseCodes.PARAM_CONTAIN_ERROR)
    @Digits(integer = 12, fraction = 4, message = ResponseCodes.PARAM_CONTAIN_ERROR)
    @JsonProperty("JPPrize")
    public BigDecimal JpPrize;

    @NotNull(message = ResponseCodes.PARAM_CONTAIN_ERROR)
    @Digits(integer = 12, fraction = 4, message = ResponseCodes.PARAM_CONTAIN_ERROR)
    @JsonProperty("NetWin")
    public BigDecimal netWin;

    @Digits(integer = 12, fraction = 4, message = ResponseCodes.PARAM_CONTAIN_ERROR)
    @JsonProperty("RequireAmt")
    public BigDecimal requireAmt;

    @NotBlank(message = ResponseCodes.DATE_INPUT_MISSING)
    @JsonProperty("GameDate")
    public String gameDate;

    @NotBlank(message = ResponseCodes.DATE_INPUT_MISSING)
    @JsonProperty("CreateDate")
    public String createDate;

    @NotNull(message = ResponseCodes.PARAM_CONTAIN_ERROR)
    @Digits(integer = 13, fraction = 0, message = ResponseCodes.PARAM_CONTAIN_ERROR)
    @JsonProperty("Ts")
    public BigDecimal ts;

}
