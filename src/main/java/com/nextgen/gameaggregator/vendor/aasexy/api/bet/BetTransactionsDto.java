package com.nextgen.gameaggregator.vendor.aasexy.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.vendor.aasexy.dto.GameInfoDto;
import com.nextgen.gameaggregator.vendor.aasexy.service.VendorService;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetTransactionsDto implements BetResultData {
    @NotBlank
    @Size(max = 255)
    private String platformTxId;

    @NotBlank
    @Size(max = 50)
    private String userId;

    @NotBlank
    private String currency;

    private String platform;

    private String gameType;

    @NotBlank
    @Size(max = 255)
    private String gameCode;

    private String gameName;

    @JsonProperty("betType")
    private String betTypes;

    @NotNull
    @Digits(integer = 20, fraction = 8)
    private BigDecimal betAmount;

    private String betTime;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("roundId")
    private String roundId;

    private GameInfoDto gameInfo;

    @Override
    public String getExternalTransactionId() {
        return this.platformTxId;
    }

    @Override
    public String getVendorBetId() {
        return this.platformTxId;
    }

    @Override
    public String getRoundId() {
        return this.roundId;
    }

    @Override
    public String getGameId() {
        return this.gameCode;
    }

    @Override
    public BigDecimal getBetAmount() {
        return this.betAmount;
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
        return this.betAmount;
    }

    @Override
    public Long getVendorBetTime() {
       return VendorService.getTimeStamp(this.betTime);
    }

    @Override
    public Long getResultTime() {
        return null;
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
}
