package com.nextgen.gameaggregator.vendor.playngo.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.vendor.playngo.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.playngo.service.VendorService;
import jakarta.validation.constraints.*;
import lombok.Data;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Data
@JacksonXmlRootElement(localName = "reserve")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReserveDto extends CommonDto implements BetResultData {

    @NotBlank
    @Size(min = 1, max = 64)
    @JacksonXmlProperty(localName = "externalId")
    private String externalId;

    @NotBlank
    @Size(min = 1, max = 32)
    @JacksonXmlProperty(localName = "transactionId")
    private String transactionId;

    @NotNull
    @Positive
    @Digits(integer = 13, fraction = 2)
    @JacksonXmlProperty(localName = "real")
    private BigDecimal real;

    @NotBlank
    @Size(min = 3, max = 3)
    @Pattern(regexp = "^[a-zA-Z]+$")
    @JacksonXmlProperty(localName = "currency")
    private String currency;

    @NotBlank
    @Size(min = 1, max = 16)
    @JacksonXmlProperty(localName = "gameSessionId")
    private String gameSessionId;

    @Size(max = 50)
    @JacksonXmlProperty(localName = "contextId")
    private String contextId;

    @NotNull
    @PositiveOrZero
    @JacksonXmlProperty(localName = "roundId")
    private Long roundId;

    @Pattern(regexp = "^(0|1|2)$")
    @JacksonXmlProperty(localName = "gameMode")
    private String gameMode;

    @NotBlank
    @Pattern(regexp = "^(1|2|5)$")
    @JacksonXmlProperty(localName = "channel")
    private String channel;

    @Size(max = 32)
    @JacksonXmlProperty(localName = "freegameExternalId")
    private String freeGameExternalId;

    @NotNull
    @Positive
    @Digits(integer = 13, fraction = 2)
    @JacksonXmlProperty(localName = "actualValue")
    private BigDecimal actualValue;

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
        return this.real;
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
        return this.real;
    }

    @Override
    public Long getVendorBetTime() {
        return VendorService.getTimestamp();
    }

    @Override
    public Long getResultTime() {
        return VendorService.getTimestamp();
    }

    @Override
    public Long getVendorSettleTime() {
        return VendorService.getTimestamp();
    }

    @Override
    public BigDecimal getJackpotAmount() {
        return BigDecimal.ZERO;
    }

    @Override
    public Integer getIsFreespin() {
        // Check condition to know this bet is free spin or not
        if (Objects.equals(this.getReal(), BigDecimal.ZERO)) {
            return 1;
        }
        return 0;
    }

    @Override
    public BetStatus getBetStatus() {
        return BetStatus.UNSETTLED;
    }
}
