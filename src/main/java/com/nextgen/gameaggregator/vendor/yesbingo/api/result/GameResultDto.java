package com.nextgen.gameaggregator.vendor.yesbingo.api.result;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
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
    @NotNull
    @Positive
    public Long transferId;

    // player id
    @NotBlank
    @Pattern(regexp = "^[a-z0-9]+$")
    public String uid;

    // Game serial number (unique value according to game type)
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
    @NotBlank
    public String gameDate;

    // Report Date ISO 8601
    @NotBlank
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
    @PositiveOrZero
    public BigDecimal win;

    // Net win amount
    @NotNull
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
    @NotBlank
    public String lastModifyTime;


    // ----- For Slot game (gType = 1) -----------
    // Jackpot Contribution
    @PositiveOrZero
    public BigDecimal jackpotWin;

    @NegativeOrZero
    public BigDecimal jackpotContribute;

    // ----- For Bingo game (gType = 3) -----------
    @Positive
    public Long playSeq;

    @Positive
    public Integer round;

    @Override
    public String getExternalTransactionId() {
        return this.transferId.toString();
    }

    @Override
    public String getVendorBetId() {
        return this.transferId.toString();
    }

    @Override
    public String getRoundId() {
        return this.transferId.toString();
    }

    @Override
    public String getGameId() {
        return this.gType.toString() + "_" + this.mType.toString();
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
        return this.netWin;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return BigDecimal.ZERO;
    }

    @Override
    public Long getVendorBetTime() {
        return Instant.parse(this.gameDate).toEpochMilli();
    }

    @Override
    public Long getResultTime() {
        return Instant.parse(this.gameDate).toEpochMilli();
    }

    @Override
    public Long getVendorSettleTime() {
        return Instant.parse(this.gameDate).toEpochMilli();
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
