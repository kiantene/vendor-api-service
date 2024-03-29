package com.nextgen.gameaggregator.vendor.facai.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.facai.constant.ResponseCodes;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetDto implements BetResultData {

    @NotBlank(message = ResponseCodes.PARAM_CONTAIN_ERROR)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.PARAM_CONTAIN_ERROR)
    @Size(min = 1, max = 24, message = ResponseCodes.PARAM_CONTAIN_ERROR)
    @JsonProperty("RecordID")
    public String recordID;

    @NotBlank(message = ResponseCodes.PARAM_CONTAIN_ERROR)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.PARAM_CONTAIN_ERROR)
    @Size(min = 1, max = 24, message = ResponseCodes.PARAM_CONTAIN_ERROR)
    @JsonProperty("BankID")
    public String bankID;

    @NotBlank(message = ResponseCodes.PLAYER_NOT_FOUND)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.PLAYER_NOT_FOUND)
    @Size(min = 2, max = 30, message = ResponseCodes.PLAYER_NOT_FOUND)
    @JsonProperty("MemberAccount")
    public String memberAccount;

    @NotBlank(message = ResponseCodes.CURRENCY_MISSING)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.CURRENCY_MISSING)
    @Size(min = 3, max = 4, message = ResponseCodes.CURRENCY_MISSING)
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

    @Override
    public String getExternalTransactionId() {
        return this.bankID;
    }

    @Override
    public String getVendorBetId() {

        //if handle fish
        if(this.gameType.equals(1)){
//            long dateChangeVendorBetId = 1711929600000L; // April 01 2024 GMT + 0
            return (this.getVendorBetTime() < 1709222400000L) ? recordID : bankID;
        }

        // if handle slot
        return this.recordID;
    }

    @Override
    public String getRoundId() {
        return this.recordID;
    }

    @Override
    public String getGameId() {
        return Integer.toString(this.gameID);
    }

    @Override
    public BigDecimal getBetAmount() {
        return this.bet;
    }

    @Override
    public BigDecimal getWinAmount() {
        return this.win;
    }

    @Override
    public BigDecimal getWinLoss() {
        return (this.win.subtract(this.bet));
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return this.bet;
    }

    @Override
    public Long getVendorBetTime() {
        //convert date time string to timestamp
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime localDateTime = LocalDateTime.parse(this.getCreateDate(), formatter);
        ZonedDateTime zonedDateTime = ZonedDateTime.of(localDateTime, ZoneId.of("UTC-4"));
        long timestamp = zonedDateTime.toInstant().toEpochMilli();
        return timestamp;
    }

    @Override
    public Long getResultTime() {
        //convert date time string to timestamp
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime localDateTime = LocalDateTime.parse(this.getGameDate(), formatter);
        ZonedDateTime zonedDateTime = ZonedDateTime.of(localDateTime, ZoneId.of("UTC-4"));
        long timestamp = zonedDateTime.toInstant().toEpochMilli();
        return timestamp;
    }

    @Override
    public Long getVendorSettleTime() {
        //convert date time string to timestamp
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime localDateTime = LocalDateTime.parse(this.getGameDate(), formatter);
        ZonedDateTime zonedDateTime = ZonedDateTime.of(localDateTime, ZoneId.of("UTC-4"));
        long timestamp = zonedDateTime.toInstant().toEpochMilli();
        return timestamp;
    }

    @Override
    public BigDecimal getJackpotAmount() {
        return this.JpPrize;
    }

    @Override
    public Integer getIsFreespin() {
        return 0;
    }

    /**
     * @return
     */
    @Override
    public BetStatus getBetStatus() {
        return BetStatus.SETTLED;
    }
}