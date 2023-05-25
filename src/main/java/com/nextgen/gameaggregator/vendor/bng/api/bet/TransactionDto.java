package com.nextgen.gameaggregator.vendor.bng.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionDto implements BetResultData {
    private String name;
    private String uid;
    private String token;
    private String session;
    private String game_id;
    private String game_name;
    private String provider_id;
    private String provider_name;
    private String c_at;
    private String sent_at;
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
        return new BigDecimal(this.getArgs().getWin());
    }

    @Override
    public BigDecimal getWinLoss() {

        // Convert win and bet amount into BigDecimal format
        BigDecimal betAmount = this.getArgs().getBet() == null ? BigDecimal.ZERO : new BigDecimal(this.getArgs().getBet());
        BigDecimal winAmount = new BigDecimal(this.getArgs().getWin());

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
    public Long getResultTime() { return getTimeStamp(this.getSent_at()); }

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
        if (this.getArgs().getRound_finished()) {
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
