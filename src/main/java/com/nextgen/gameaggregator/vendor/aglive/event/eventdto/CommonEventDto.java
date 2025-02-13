package com.nextgen.gameaggregator.vendor.aglive.event.eventdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "Record")
public class CommonEventDto implements BetResultData {

    @JacksonXmlProperty(localName = "Record")
    private EventDto eventDto;

    @Override
    public String getExternalTransactionId() {
        return this.eventDto.getTransactionID();
    }

    @Override
    public String getVendorBetId() {
        return this.eventDto.getTransactionID();
    }

    @Override
    public String getRoundId() {
        return this.eventDto.getTransactionID();
    }

    @Override
    public String getGameId() {
        return this.eventDto.getEventID();
    }

    @Override
    public BigDecimal getBetAmount() {
        if ("WITHDRAW".equals(eventDto.getTransactionType())) {
            return this.eventDto.getAmount();
        } else {
            return null;
        }
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
        return null;
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
