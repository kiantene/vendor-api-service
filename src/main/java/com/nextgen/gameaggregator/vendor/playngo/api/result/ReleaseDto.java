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

    @NotBlank
    @Pattern(regexp = "^(0|1)$")
    @JacksonXmlProperty(localName = "state")
    private String state;

    @PositiveOrZero
    @Digits(integer = 13, fraction = 2)
    @JacksonXmlProperty(localName = "totalLoss")
    private String totalLoss;

    @PositiveOrZero
    @Digits(integer = 13, fraction = 2)
    @JacksonXmlProperty(localName = "totalGain")
    private String totalGain;

    @PositiveOrZero
    @JacksonXmlProperty(localName = "numRounds")
    private Integer numRounds;

    // 0 = Real Money, 1 = Promotional money. For example, the result of Free game spins.
    @NotBlank
    @Pattern(regexp = "^(0|1)$")
    @JacksonXmlProperty(localName = "type")
    private String type;

    @NotNull
    @PositiveOrZero
    @JacksonXmlProperty(localName = "roundId")
    private Long roundId;

    @NotNull
    @PositiveOrZero
    @Digits(integer = 13, fraction = 2)
    @JacksonXmlProperty(localName = "jackpotGain")
    private BigDecimal jackpotGain;

    @NotNull
    @PositiveOrZero
    @Digits(integer = 13, fraction = 2)
    @JacksonXmlProperty(localName = "jackpotLoss")
    private BigDecimal jackpotLoss;

    @NotNull
    @PositiveOrZero
    @Digits(integer = 13, fraction = 2)
    @JacksonXmlProperty(localName = "jackpotGainSeed")
    private BigDecimal jackpotGainSeed;

    @NotNull
    @PositiveOrZero
    @JacksonXmlProperty(localName = "jackpotGainId")
    private Integer jackpotGainId;

    @Size(max = 32)
    @JacksonXmlProperty(localName = "freegameExternalId")
    private String freeGameExternalId;

    @NotNull
    @PositiveOrZero
    @JacksonXmlProperty(localName = "turnover")
    private BigDecimal turnover;

    @NotNull
    @Pattern(regexp = "^(0|1)$")
    @JacksonXmlProperty(localName = "freegameFinished")
    private Integer freeGameFinished;

    @NotNull
    @PositiveOrZero
    @Digits(integer = 13, fraction = 2)
    @JacksonXmlProperty(localName = "freegameGain")
    private BigDecimal freeGameGain;

    @NotNull
    @PositiveOrZero
    @Digits(integer = 13, fraction = 2)
    @JacksonXmlProperty(localName = "freegameLoss")
    private BigDecimal freeGameLoss;

    @Pattern(regexp = "^(0|1|2)$")
    @JacksonXmlProperty(localName = "gameMode")
    private String gameMode;

    @NotBlank
    @Pattern(regexp = "^(1|2|5)$")
    @JacksonXmlProperty(localName = "channel")
    private String channel;

    @PositiveOrZero
    @Digits(integer = 13, fraction = 2)
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
        return 0;
    }

    @Override
    public BetStatus getBetStatus() {
        return BetStatus.SETTLED;
    }
}
