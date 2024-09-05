package com.nextgen.gameaggregator.vendor.cg.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetDto implements BetResultData {
    @NotBlank
    @Size(max = 255)
    public String channelId;
    @NotBlank
    @Size(max = 50)
    public String accountId;
    @NotBlank
    @Size(max = 255)
    public String gameType;
    @NotBlank
    @Size(max = 255)
    public String roundId;
    @NotNull
    @Digits(integer = 20, fraction = 8)
    public BigDecimal amount;
    @NotBlank
    @Size(max = 5)
    public String currency;
    @NotBlank
    @Size(max = 255)
    public String mtcode;
    @NotBlank
    public String eventTime;
    public BigDecimal lockMoney;
    public String ipaddress;
    public String device;


    @Override
    public String getExternalTransactionId() {
        return this.mtcode;
    }

    @Override
    public String getVendorBetId() {
        return this.roundId;
    }

    @Override
    public String getRoundId() {
        return this.roundId;
    }

    @Override
    public String getGameId() {
        return gameType;
    }

    @Override
    public BigDecimal getBetAmount() {
        return amount;
    }

    @Override
    public BigDecimal getWinAmount() {
        return null;
    }

    @Override
    public BigDecimal getWinLoss() {
        return null;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return null;
    }

    @Override
    public Long getVendorBetTime() {
        return getTimestamp();
    }

    @Override
    public Long getResultTime() {
        return getTimestamp();
    }

    @Override
    public Long getVendorSettleTime() {
        return null;
    }

    @Override
    public BigDecimal getJackpotAmount() {
        return null;
    }

    @Override
    public Integer getIsFreespin() {
        return 0;
    }

    @Override
    public BetStatus getBetStatus() {
        return BetStatus.UNSETTLED;
    }


    public Long getTimestamp() {
        Instant instant = Instant.parse(this.getEventTime());
        return instant.toEpochMilli();
    }
}
