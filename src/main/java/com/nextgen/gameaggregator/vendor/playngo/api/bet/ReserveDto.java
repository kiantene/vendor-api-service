package com.nextgen.gameaggregator.vendor.playngo.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.vendor.playngo.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.playngo.service.VendorService;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JacksonXmlRootElement(localName = "reserve")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReserveDto extends CommonDto implements BetResultData {
    @NotBlank
    @JacksonXmlProperty(localName = "externalId")
    private String externalId;
    @NotBlank
    @JacksonXmlProperty(localName = "productId")
    private String productId;
    @NotBlank
    @JacksonXmlProperty(localName = "transactionId")
    private String transactionId;
    @NotBlank
    @JacksonXmlProperty(localName = "real")
    private String real;
    @NotBlank
    @JacksonXmlProperty(localName = "currency")
    private String currency;
    @NotBlank
    @JacksonXmlProperty(localName = "gameId")
    private String gameId;
    @NotBlank
    @JacksonXmlProperty(localName = "gameSessionId")
    private String gameSessionId;
    @JacksonXmlProperty(localName = "contextId")
    private String contextId;
    @NotBlank
    @JacksonXmlProperty(localName = "accessToken")
    private String accessToken;
    @NotBlank
    @JacksonXmlProperty(localName = "roundId")
    private String roundId;
    @JacksonXmlProperty(localName = "gameMode")
    private String gameMode;
    @NotBlank
    @JacksonXmlProperty(localName = "channel")
    private String channel;
    @JacksonXmlProperty(localName = "freegameExternalId")
    private String freeGameExternalId;
    @JacksonXmlProperty(localName = "actualValue")
    private String actualValue;

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
