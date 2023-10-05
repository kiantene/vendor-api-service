package com.nextgen.gameaggregator.vendor.yesbingo.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.vendor.yesbingo.constant.GameTypes;
import com.nextgen.gameaggregator.vendor.yesbingo.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.yesbingo.service.VendorService;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetDto implements BetResultData {

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

    // Game serial number (unique value according to game type) - round id for Slot
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

    // Vendor's defined currency
    @NotBlank
    @Size(min = 3, max = 3)
    @Pattern(regexp = "[a-zA-Z]+")
    public String currency;

    // Bet amount
    @NotNull
    @NegativeOrZero
    public BigDecimal bet;

    // Participated in Mystery Prizes
    @NotNull
    @Pattern(regexp = "^true$|^false$")
    public String isJoinMysteryJackpot;

    // Jackpot Contribution
    @NotNull
    @NegativeOrZero
    public BigDecimal jackpotContribute;

    // ----- For Bingo game (gType = 3) -----------

    // round id for Bingo games
    @Positive
    private Long playSeq;

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
                this.setBetId(this.getTransferId().toString());
            }
            case GameTypes.BINGO -> {
                this.setBetId(this.gameSeqNo);
            }
        }
        return this.betId;
    }

    @Override
    public String getRoundId() {
        switch (this.gType) {
            case GameTypes.SLOT -> {
                this.setRoundId(this.gameSeqNo);
            }
            case GameTypes.BINGO -> {
                this.setRoundId(this.getPlaySeq().toString());
            }
        }
        return this.roundId;
    }

    @Override
    public String getGameId() {
        return this.gType.toString() + "_" + this.mType.toString();
    }

    @Override
    public BigDecimal getBetAmount() {
        return this.bet.negate();
    }

    @Override
    public BigDecimal getWinAmount() {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getWinLoss() {
        return getBetAmount().negate();
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return getBetAmount();
    }

    @Override
    public Long getVendorBetTime() {
        return VendorService.getCurrentTime(this.gameDate);
    }

    @Override
    public Long getResultTime() {
        return VendorService.getCurrentTime(this.gameDate);
    }

    @Override
    public Long getVendorSettleTime() {
        return VendorService.getCurrentTime(this.gameDate);
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
        return BetStatus.UNSETTLED;
    }
}
