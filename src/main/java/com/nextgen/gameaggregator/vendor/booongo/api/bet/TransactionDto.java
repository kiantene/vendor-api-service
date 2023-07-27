package com.nextgen.gameaggregator.vendor.booongo.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionDto implements BetResultData {

    @NotBlank
    private String name;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(max = 35)
    private String uid;

    @NotBlank
    private String token;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 32, max = 32)
    private String session;

    @NotBlank
    private String game_id;

    @NotBlank
    private String game_name;

    @NotBlank
    private String provider_id;

    @NotBlank
    private String provider_name;

    @NotBlank
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(Z|[+-]\\d{2}:\\d{2})$")
    private String c_at;

    @NotBlank
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(Z|[+-]\\d{2}:\\d{2})$")
    // vendor record the time while sent data
    private String sent_at;

    @NotNull
    private TransactionArgsDto args;

    @Override
    public String getExternalTransactionId() {
        return this.getUid();
    }

    @Override
    public String getVendorBetId() {
        return this.getUid();
    }

    @Override
    public String getRoundId() {
        return String.valueOf(this.getArgs().getRound_id());
    }

    @Override
    public String getGameId() {
        return this.getGame_id();
    }

    @Override
    public BigDecimal getBetAmount() {
        return this.getArgs().getBet() == null ? BigDecimal.ZERO : new BigDecimal(this.getArgs().getBet());
    }

    @Override
    public BigDecimal getWinAmount() {
        return this.getArgs().getWin() == null ? BigDecimal.ZERO : new BigDecimal(this.getArgs().getWin());
    }

    @Override
    public BigDecimal getWinLoss() {

        // Convert win and bet amount into BigDecimal format
        BigDecimal betAmount = this.getArgs().getBet() == null ? BigDecimal.ZERO : new BigDecimal(this.getArgs().getBet());
        BigDecimal winAmount = this.getArgs().getWin() == null ? BigDecimal.ZERO : new BigDecimal(this.getArgs().getWin());

        // Return the value by (winAmount - betAmount)
        return winAmount.subtract(betAmount);
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return this.getArgs().getBet() == null ? BigDecimal.ZERO : new BigDecimal(this.getArgs().getBet());
    }

    @Override
    public Long getVendorBetTime() { return getTimeStamp(this.getC_at()); }

    @Override
    public Long getResultTime() { return getTimeStamp(this.getC_at()); }

    @Override
    public Long getVendorSettleTime() { return getTimeStamp(this.getC_at()); }

    @Override
    public BigDecimal getJackpotAmount() {
        return BigDecimal.ZERO;
    }

    @Override
    public Integer getIsFreespin() {

        int freeSpin = 0;

        // Check condition to know this bet is free spin or not
        if (this.getArgs().getBet() == null) {
            freeSpin = 1;
        }

        return freeSpin;
    }

    @Override
    public BetStatus getBetStatus() {

        // Check condition to decide it is settled or unsettle
        if (this.getArgs().getRound_finished() == true) {
            return BetStatus.SETTLED;
        } else {
            return BetStatus.UNSETTLED;
        }
    }

    public Long getTimeStamp(String datetime) {
        Instant instant = Instant.parse(datetime);
        return instant.toEpochMilli();
    }
}
