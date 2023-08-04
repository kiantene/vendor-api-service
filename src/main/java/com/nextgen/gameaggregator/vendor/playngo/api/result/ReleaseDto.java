package com.nextgen.gameaggregator.vendor.playngo.api.result;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.vendor.playngo.dto.CommonDto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.List;

@Data
@JacksonXmlRootElement(localName = "release")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReleaseDto extends CommonDto implements BetResultData {

    @NotBlank
    @JacksonXmlProperty(localName = "externalId")
    private String externalId;
    @NotBlank
    @JacksonXmlProperty(localName = "productId")
    private String productId;
    @NotBlank
    @JacksonXmlProperty(localName = "transactionId")
    private String transactionId;
    @JacksonXmlProperty(localName = "retry")
    private String retry;
    @NotBlank
    @JacksonXmlProperty(localName = "real")
    private String real;
    @NotBlank
    @JacksonXmlProperty(localName = "currency")
    private String currency;
    @NotBlank
    @JacksonXmlProperty(localName = "gameSessionId")
    private String gameSessionId;
    @JacksonXmlProperty(localName = "contextId")
    private String contextId;
    @NotBlank
    @JacksonXmlProperty(localName = "state")
    private String state;
    @JacksonXmlProperty(localName = "totalLoss")
    private String totalLoss;
    @JacksonXmlProperty(localName = "totalGain")
    private String totalGain;
    @JacksonXmlProperty(localName = "numRounds")
    private String numRounds;
    @NotBlank
    @JacksonXmlProperty(localName = "type")
    private String type;
    @NotBlank
    @JacksonXmlProperty(localName = "gameId")
    private String gameId;
    @NotBlank
    @JacksonXmlProperty(localName = "accessToken")
    private String accessToken;
    @NotBlank
    @JacksonXmlProperty(localName = "roundId")
    private String roundId;
    @JacksonXmlProperty(localName = "jackpotGain")
    private String jackpotGain;
    @JacksonXmlProperty(localName = "jackpotLoss")
    private String jackpotLoss;
    @JacksonXmlProperty(localName = "jackpotGainSeed")
    private String jackpotGainSeed;
    @JacksonXmlProperty(localName = "jackpotGainId")
    private String jackpotGainId;
    @JacksonXmlProperty(localName = "freegameExternalId")
    private String freeGameExternalId;
    @NotBlank
    @JacksonXmlProperty(localName = "turnover")
    private String turnover;
    @JacksonXmlProperty(localName = "freegameFinished")
    private String freeGameFinished;
    @JacksonXmlProperty(localName = "freegameGain")
    private String freeGameGain;
    @JacksonXmlProperty(localName = "freegameLoss")
    private String freeGameLoss;
    @JacksonXmlProperty(localName = "gameMode")
    private String gameMode;
    @NotBlank
    @JacksonXmlProperty(localName = "channel")
    private String channel;
    @JacksonXmlProperty(localName = "freegameTotalGain")
    private String freeGameTotalGain;

    @Nullable
    @JacksonXmlProperty(localName = "jackpots")
    @JacksonXmlElementWrapper(localName = "jackpots")
    private List<JackpotDto> jackpots;

    @Override
    public String getExternalTransactionId() {
        return this.transactionId;
    }

    @Override
    public String getVendorBetId() {
        return this.transactionId;
    }

    @Override
    public String getRoundId() {
        return this.roundId;
    }

    @Override
    public String getGameId() {
        return this.gameId;
    }

    @Override
    public BigDecimal getBetAmount() {
        return null;
    }

    @Override
    public BigDecimal getWinAmount() {
        return new BigDecimal(this.real);
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
        return null;
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
        // Check condition to know this bet have jackpot win or not
        if (this.jackpotGain != null) {
            return new BigDecimal(this.jackpotGain);
        }
        return BigDecimal.ZERO;
    }

    @Override
    public Integer getIsFreespin() {
        // Check condition to know this bet is free spin or not
        if (this.freeGameExternalId != null) {
            return 1;
        }
        return 0;
    }

    @Override
    public BetStatus getBetStatus() {
        // Check condition to know this free spin is finished or not
        if (this.freeGameExternalId != null && !this.freeGameExternalId.isEmpty()
                && this.freeGameFinished != null && this.freeGameFinished != "1") {
            return BetStatus.UNSETTLED;
        }
        return BetStatus.SETTLED;
    }
}
