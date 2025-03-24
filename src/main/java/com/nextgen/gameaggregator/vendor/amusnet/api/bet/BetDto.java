package com.nextgen.gameaggregator.vendor.amusnet.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.vendor.amusnet.dto.CommonDto;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@JacksonXmlRootElement(localName = "WithdrawRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetDto extends CommonDto implements BetResultData {

    @Override
    public String getExternalTransactionId() {
        return this.getTransferId();
    }

    @Override
    public String getVendorBetId() {
        return this.getTransferId();
    }

    @Override
    public String getRoundId() {
        return this.getGameNumber();
    }

    @Override
    public String getGameId() {
        return this.getVendorGameId();
    }

    @Override
    public BigDecimal getBetAmount() {
        return this.getAmount();
    }

    @Override
    public BigDecimal getWinAmount() {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getWinLoss() {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return this.getAmount();
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
