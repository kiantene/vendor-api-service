package com.nextgen.gameaggregator.vendor.aglive.api.endround;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "Data")
public class CommonSettleDto implements BetResultData {
    @JacksonXmlProperty(localName = "Record")
    private SettleDto settleDto;

    @Override
    public String getExternalTransactionId() {

        if (this.settleDto.getGameType().equals("ROU")) {
            return this.settleDto.getBillNo();
        } else {
            return this.settleDto.getTransactionID();
        }
    }

    @Override
    public String getVendorBetId() {
        if (this.settleDto.getGameType().equals("ROU")) {
            return this.settleDto.getBillNo();
        } else {
            return this.settleDto.getTransactionID();
        }
    }

    @Override
    public String getRoundId() {
        return this.settleDto.getGameCode();
    }

    @Override
    public String getGameId() {
        return null;
    }

    @Override
    public BigDecimal getBetAmount() {
        return null;
    }

    @Override
    public BigDecimal getWinAmount() {
        return this.settleDto.getValidBetAmount().add(this.settleDto.getNetAmount());
    }

    @Override
    public BigDecimal getWinLoss() {
        return this.getWinAmount();
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return null;
    }

    @Override
    public Long getVendorBetTime() {
        return System.currentTimeMillis();
    }

    @Override
    public Long getResultTime() {
        return null;
    }

    @Override
    public Long getVendorSettleTime() {
        return System.currentTimeMillis();
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
        if ("true".equals(settleDto.getFinish())) {
            return BetStatus.SETTLED;
        }
        return BetStatus.UNSETTLED;
    }
}
