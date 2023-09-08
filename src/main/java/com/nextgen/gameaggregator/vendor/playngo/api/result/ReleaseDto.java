package com.nextgen.gameaggregator.vendor.playngo.api.result;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.vendor.playngo.dto.CommonDto;
import jakarta.validation.constraints.*;
import lombok.Data;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.List;

@Data
@JacksonXmlRootElement(localName = "release")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReleaseDto extends CommonDto implements BetResultData {

    @NotBlank
    @Size(min = 1, max = 64)
    @JacksonXmlProperty(localName = "externalId")
    private String externalId;

    @NotBlank
    @Size(min = 1, max = 32)
    @JacksonXmlProperty(localName = "transactionId")
    private String transactionId;

    @Pattern(regexp = "^(0|1)$")
    @JacksonXmlProperty(localName = "retry")
    private String retry;

    @NotNull
    @PositiveOrZero
    @JacksonXmlProperty(localName = "real")
    private BigDecimal real;

    @NotBlank
    @Size(min = 3, max = 3)
    @Pattern(regexp = "^[a-zA-Z]+$")
    @JacksonXmlProperty(localName = "currency")
    private String currency;

    @NotBlank
    @Size(min = 1, max = 32)
    @JacksonXmlProperty(localName = "gameSessionId")
    private String gameSessionId;

    @Size(max = 50)
    @JacksonXmlProperty(localName = "contextId")
    private String contextId;

    @NotNull
    @Pattern(regexp = "^(0|1)$")
    @JacksonXmlProperty(localName = "state")
    private String state;

    @PositiveOrZero
    @JacksonXmlProperty(localName = "totalLoss")
    private String totalLoss;

    @PositiveOrZero
    @JacksonXmlProperty(localName = "totalGain")
    private String totalGain;

    @PositiveOrZero
    @JacksonXmlProperty(localName = "numRounds")
    private String numRounds;

    // 0 = Real Money, 1 = Promotional money. For example, the result of Free game spins.
    @NotNull
    @Pattern(regexp = "^(0|1)$")
    @JacksonXmlProperty(localName = "type")
    private String type;

    @NotNull
    @PositiveOrZero
    @JacksonXmlProperty(localName = "roundId")
    private Long roundId;

    @NotNull
    @PositiveOrZero
    @JacksonXmlProperty(localName = "jackpotGain")
    private BigDecimal jackpotGain;

    @NotNull
    @PositiveOrZero
    @JacksonXmlProperty(localName = "jackpotLoss")
    private BigDecimal jackpotLoss;

    @NotNull
    @PositiveOrZero
    @JacksonXmlProperty(localName = "jackpotGainSeed")
    private String jackpotGainSeed;

    @NotNull
    @PositiveOrZero
    @JacksonXmlProperty(localName = "jackpotGainId")
    private String jackpotGainId;

    @Size(max = 32)
    @JacksonXmlProperty(localName = "freegameExternalId")
    private String freeGameExternalId;

    @PositiveOrZero
    @JacksonXmlProperty(localName = "turnover")
    private BigDecimal turnover;

    @Pattern(regexp = "^(0|1)$")
    @JacksonXmlProperty(localName = "freegameFinished")
    private String freeGameFinished;

    @PositiveOrZero
    @JacksonXmlProperty(localName = "freegameGain")
    private BigDecimal freeGameGain;

    @PositiveOrZero
    @JacksonXmlProperty(localName = "freegameLoss")
    private String freeGameLoss;

    @Size(max = 64)
    @JacksonXmlProperty(localName = "externalGameSessionId")
    private String externalGameSessionId;

    @Pattern(regexp = "^(0|1|2)$")
    @JacksonXmlProperty(localName = "gameMode")
    private String gameMode;

    @NotBlank
    @Pattern(regexp = "^(1|2|5)$")
    @JacksonXmlProperty(localName = "channel")
    private String channel;

    @PositiveOrZero
    @JacksonXmlProperty(localName = "freegameTotalGain")
    private BigDecimal freeGameTotalGain;

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
        return String.valueOf(this.roundId);
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
        return this.real;
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
            return this.jackpotGain;
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
                && this.freeGameFinished != null && !this.freeGameFinished.equals("1")) {
            return BetStatus.UNSETTLED;
        }
        return BetStatus.SETTLED;
    }
}
