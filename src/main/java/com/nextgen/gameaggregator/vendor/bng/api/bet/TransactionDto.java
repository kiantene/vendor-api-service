package com.nextgen.gameaggregator.vendor.bng.api.bet;

import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import lombok.Data;
import com.nextgen.gameaggregator.enums.BetStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data
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
        return this.getSession();
    }

    @Override
    public String getVendorBetId() {
        return this.getUid();
    }

    @Override
    public String getRoundId() {
        return this.getArgs().getRound_id().toString();
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

        BigDecimal winloss = new BigDecimal(this.getArgs().getWin());
        BigDecimal bet = new BigDecimal(this.getArgs().getBet());

        return winloss.subtract(bet);
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return null;
    }

    @Override
    public Long getVendorBetTime() {
        // Convert string into datetime format
        LocalDateTime dateTime = LocalDateTime.parse(this.getC_at());

        // Convert datetime into unix time format
        long unixTimestamp = dateTime.toEpochSecond(ZoneOffset.UTC);

        return unixTimestamp;
    }

    @Override
    public Long getResultTime() {
        // Convert string into datetime format
        LocalDateTime dateTime = LocalDateTime.parse(this.getSent_at());

        // Convert datetime into unix time format
        long unixTimestamp = dateTime.toEpochSecond(ZoneOffset.UTC);

        return unixTimestamp;
    }

    @Override
    public Long getVendorSettleTime() {

        // Convert string into datetime format
        LocalDateTime dateTime = LocalDateTime.parse(this.getSent_at());

        // Convert datetime into unix time format
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
