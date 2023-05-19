package com.nextgen.gameaggregator.vendor.bng.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import lombok.Data;

import java.math.BigDecimal;
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
        return this.getSession();
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
        return new BigDecimal(this.getArgs().getBet());
    }

    @Override
    public BigDecimal getWinAmount() {
        return new BigDecimal(this.getArgs().getWin());
    }

    @Override
    public BigDecimal getWinLoss() {

        // Convert win and bet amount into BigDecimal format
        BigDecimal betAmount = new BigDecimal(this.getArgs().getBet());
        BigDecimal winAmount = new BigDecimal(this.getArgs().getWin());

        // Return the value by (winAmount - betAmount)
        return winAmount.subtract(betAmount);
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return new BigDecimal(this.getArgs().getBet());
    }

    @Override
    public Long getVendorBetTime() {

        // Convert string datetime into LocalDateTime format
        LocalDateTime dateTime = LocalDateTime.parse(this.getC_at());

        // Convert LocalDateTime into unixTimestamp format
        long unixTimestamp = dateTime.toEpochSecond(ZoneOffset.UTC);

        return unixTimestamp;
    }

    @Override
    public Long getResultTime() {
        // Convert string datetime into LocalDateTime format
        LocalDateTime dateTime = LocalDateTime.parse(this.getSent_at());

        // Convert LocalDateTime into unixTimestamp format
        long unixTimestamp = dateTime.toEpochSecond(ZoneOffset.UTC);

        return unixTimestamp;
    }

    @Override
    public Long getVendorSettleTime() {
        // Convert string datetime into LocalDateTime format
        LocalDateTime dateTime = LocalDateTime.parse(this.getSent_at());

        // Convert LocalDateTime into unixTimestamp format
        long unixTimestamp = dateTime.toEpochSecond(ZoneOffset.UTC);

        return unixTimestamp;
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
