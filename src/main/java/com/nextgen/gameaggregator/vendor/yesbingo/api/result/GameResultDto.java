package com.nextgen.gameaggregator.vendor.yesbingo.api.result;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.vendor.yesbingo.constant.GameTypes;
import com.nextgen.gameaggregator.vendor.yesbingo.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.yesbingo.service.VendorService;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameResultDto implements BetResultData {

    // Already validated in GeneralAction. Action id to bet
    public Integer action;

    // timestamp
    @NotNull
    @Positive
    public Long ts;

    // This is the bet id
    // The value used for refund
    @NotNull
    @Positive
    public Long transferId;

    // player id
    @NotBlank
    @Pattern(regexp = "^[a-z0-9]+$")
    public String uid;

    // Game serial number (unique value according to game type) - round id for Slot
    // The value used to search from vendor's BO
    @NotNull
    @Positive
    public String gameSeqNo;

    // game type
    @NotNull
    @Positive
    public Integer gType;

    // game code
    @NotNull
    @Positive
    public Integer mType;

    // Game Date ISO 8601
    @NotBlank(message = ResponseCodes.WRONG_DATE_SECOND_FORMAT)
    @Size(max = 24, message = ResponseCodes.WRONG_DATE_SECOND_FORMAT)
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])T([01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d\\.\\d{3}Z$", message = ResponseCodes.WRONG_DATE_SECOND_FORMAT)
    public String gameDate;

    // Report Date ISO 8601
    @NotBlank(message = ResponseCodes.WRONG_DATE_SECOND_FORMAT)
    @Size(max = 24, message = ResponseCodes.WRONG_DATE_SECOND_FORMAT)
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])T([01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d\\.\\d{3}Z$", message = ResponseCodes.WRONG_DATE_SECOND_FORMAT)
    public String reportDate;

    // Vendor's defined currency
    @NotBlank
    @Size(min = 3, max = 3)
    @Pattern(regexp = "[a-zA-Z]+")
    public String currency;

    // Bet amount
    @NotNull
    @NegativeOrZero
    public BigDecimal bet;

    // Win amount
    @NotNull
    @PositiveOrZero(message = ResponseCodes.PARAMETER_MUST_BE_POSITIVE_INTEGER)
    public BigDecimal win;

    // Net win amount
    @NotNull
    @Digits(integer = 10, fraction = 3)
    public BigDecimal netWin;

    @NotNull
    @Range(min = 0, max = 1)
    public Integer hasFreeGame;

    @NotNull
    @Range(min = 0, max = 1)
    public Integer hasBonusGame;

    // Participated in Mystery Prizes
    @NotNull
    @Pattern(regexp = "^true$|^false$")
    public String isJoinMysteryJackpot;

    // Last modify time ISO 8601
    @NotBlank(message = ResponseCodes.WRONG_DATE_SECOND_FORMAT)
    @Size(max = 24, message = ResponseCodes.WRONG_DATE_SECOND_FORMAT)
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])T([01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d\\.\\d{3}Z$", message = ResponseCodes.WRONG_DATE_SECOND_FORMAT)
    public String lastModifyTime;

    // ----- For Slot game (gType = 1) -----------
    // Jackpot Contribution

    @PositiveOrZero
    public BigDecimal jackpotWin;

    @NegativeOrZero
    public BigDecimal jackpotContribute;

    // ----- For Bingo game (gType = 3) -----------

    // round id for Bingo games
    @PositiveOrZero
    public Long playSeq;

    @PositiveOrZero
    public Integer round;

    // This is used to change round id between Slot and Bingo
    public String roundId;

    // This is used to change vendor bet id between Slot and Bingo
    public String betId;

    @Override
    public String getExternalTransactionId() {
        return this.transferId.toString();
    }

    @Override
    public String getVendorBetId() {
        switch (this.gType) {
            case GameTypes.SLOT -> {
                betId = this.transferId.toString();
            }
            case GameTypes.BINGO -> {
                betId = this.gameSeqNo;
            }
        }
        return betId;
    }

    @Override
    public String getRoundId() {
        switch (this.gType) {
            case GameTypes.SLOT -> {
                roundId = this.gameSeqNo;
            }
            case GameTypes.BINGO -> {
                roundId = this.getPlaySeq().toString();
            }
        }
        return roundId;
    }

    @Override
    public String getGameId() {
        return this.gType.toString() + "_" + this.mType.toString();
    }

    @Override
    public BigDecimal getBetAmount() {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getWinAmount() {
        return this.win;
    }

    @Override
    public BigDecimal getWinLoss() {
        return null;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return BigDecimal.ZERO;
    }

    @Override
    public Long getVendorBetTime() {
        return null;
    }

    @Override
    public Long getResultTime() {
        return VendorService.getCurrentTime(this.reportDate);
    }

    @Override
    public Long getVendorSettleTime() {
        return VendorService.getCurrentTime(this.reportDate);
    }

    @Override
    public BigDecimal getJackpotAmount() {
        return (getJackpotWin() != null) ? getJackpotWin() : BigDecimal.ZERO;
    }

    @Override
    public Integer getIsFreespin() {
        return (getHasFreeGame() != null) ? getHasFreeGame() : 0;
    }

    @Override
    public BetStatus getBetStatus() {
        return BetStatus.SETTLED;
    }
}
