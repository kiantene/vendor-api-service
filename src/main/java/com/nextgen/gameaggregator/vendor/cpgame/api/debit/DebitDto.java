package com.nextgen.gameaggregator.vendor.cpgame.api.debit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.vendor.cpgame.dto.CommonDto;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class DebitDto extends CommonDto implements BetResultData {

    @Override
    public String getExternalTransactionId() {
        return super.getMessageDto().getBetInfo().getBetId();
    }

    @Override
    public String getVendorBetId() {
        return super.getMessageDto().getBetInfo().getBetId();
    }

    @Override
    public String getRoundId() {
        return super.getMessageDto().getBetInfo().getBetId();
    }

    @Override
    public String getGameId() {
        return super.getMessageDto().getGameId();
    }

    @Override
    public BigDecimal getBetAmount() {
        return super.getMessageDto().getBetInfo().getBetAmount();
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
        return super.getMessageDto().getBetInfo().getBetAmount();
    }

    @Override
    public Long getVendorBetTime() {
        return this.getTime() * 1000;
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
        int status = 0;
        if (super.getMessageDto().getBetInfo().getBetAmount().equals(BigDecimal.ZERO)) {
            status = 1;
        }
        return status;
    }

    @Override
    public BetStatus getBetStatus() {
        return BetStatus.UNSETTLED;
    }
}
