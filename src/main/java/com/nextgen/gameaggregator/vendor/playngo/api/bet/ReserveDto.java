package com.nextgen.gameaggregator.vendor.playngo.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.vendor.playngo.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.playngo.service.VendorService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.List;

@Data
@JacksonXmlRootElement(localName = "reserve")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReserveDto extends CommonDto implements BetResultData {
    @NotBlank
    @Size(min = 1, max = 64)
    @JacksonXmlProperty(localName = "externalId")
    private String externalId;

    @NotBlank
    @Size(min = 1, max = 16)
    @JacksonXmlProperty(localName = "productId")
    private String productId;

    @NotBlank
    @Size(min = 1, max = 32)
    @JacksonXmlProperty(localName = "transactionId")
    private String transactionId;

    @NotBlank
    @JacksonXmlProperty(localName = "real")
    private String real;

    @NotBlank
    @Size(min = 3, max = 3)
    @JacksonXmlProperty(localName = "currency")
    private String currency;

    @NotBlank
    @Size(min = 1, max = 16)
    @JacksonXmlProperty(localName = "gameId")
    private String gameId;

    @NotBlank
    @Size(min = 1, max = 16)
    @JacksonXmlProperty(localName = "gameSessionId")
    private String gameSessionId;

    @Size(min = 1, max = 50)
    @JacksonXmlProperty(localName = "contextId")
    private String contextId;

    @NotBlank
    @Size(min = 1, max = 64)
    @JacksonXmlProperty(localName = "accessToken")
    private String accessToken;

    @NotBlank
    @PositiveOrZero
    @JacksonXmlProperty(localName = "roundId")
    private Long roundId;

    @JacksonXmlProperty(localName = "gameMode")
    private Integer gameMode;

    @NotBlank
    @JacksonXmlProperty(localName = "channel")
    private String channel;

    @JacksonXmlProperty(localName = "freegameExternalId")
    private String freeGameExternalId;

    @JacksonXmlProperty(localName = "actualValue")
    private String actualValue;

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
        return new BigDecimal(this.real);
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
        return new BigDecimal(this.real);
    }

    @Override
    public Long getVendorBetTime() {
        return VendorService.getTimestamp();
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
        return BigDecimal.ZERO;
    }

    @Override
    public Integer getIsFreespin() {
        // Check condition to know this bet is free spin or not
        if (this.real == "0") {
            return 1;
        }
        return 0;
    }

    @Override
    public BetStatus getBetStatus() {
        return BetStatus.UNSETTLED;
    }
}
