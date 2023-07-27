package com.nextgen.gameaggregator.vendor.yesbingo.api.result;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameDetailResultDto implements BetResultData {

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

    // Game serial number (unique value according to game type)
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

    // Required bet amount
    @NotNull
    @NegativeOrZero
    public BigDecimal reqBet;

    // Total bet amount
    @NotNull
    @NegativeOrZero
    public BigDecimal totalBet;

    // Total win amount
    @NotNull
    @PositiveOrZero
    public BigDecimal totalWin;

    // Net win amount
    @NotNull
    @Digits(integer = 10, fraction = 3)
    public BigDecimal netWin;

    // Participated in Mystery Prizes
    @NotNull
    @Pattern(regexp = "^true$|^false$")
    public String isJoinMysteryJackpot;

    // Last modify time ISO 8601
    @NotBlank
    public String lastModifyTime;


    // ----- For Fish game (gType = 2) -----------
    @NotNull
    @PositiveOrZero
    public Integer roomType;

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
        return this.gameSeqNo.toString();
    }

    @Override
    public String getGameId() {
        return this.gType.toString() + "_" + this.mType.toString();
    }

    @Override
    public BigDecimal getBetAmount() {
        return this.totalBet.negate();
    }

    @Override
    public BigDecimal getWinAmount() {
        return this.totalWin;
    }

    @Override
    public BigDecimal getWinLoss() {
        return null;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return this.totalBet.negate();
    }

    @Override
    public Long getVendorBetTime() {
        return Instant.parse(this.reportDate).toEpochMilli();
    }

    @Override
    public Long getResultTime() {
        return Instant.parse(this.reportDate).toEpochMilli();
    }

    @Override
    public Long getVendorSettleTime() {
        return Instant.parse(this.reportDate).toEpochMilli();
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
