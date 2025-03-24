package com.nextgen.gameaggregator.vendor.amusnet.api.endround;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.vendor.amusnet.constant.Reason;
import com.nextgen.gameaggregator.vendor.amusnet.dto.CommonDto;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@JacksonXmlRootElement(localName = "DepositRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SettleDto extends CommonDto implements BetResultData {
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
        return null;
    }

    @Override
    public BigDecimal getWinAmount() {
        if (!super.getReason().equals(Reason.JACKPOT)) {
            return super.getAmount();
        }
        return BigDecimal.ZERO;
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
        return System.currentTimeMillis();
    }

    @Override
    public Long getVendorSettleTime() {
        return System.currentTimeMillis();
    }

    @Override
    public BigDecimal getJackpotAmount() {
        if (super.getReason().equals(Reason.JACKPOT)) {
            return super.getAmount();
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
