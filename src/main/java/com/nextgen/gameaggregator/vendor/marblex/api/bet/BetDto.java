package com.nextgen.gameaggregator.vendor.marblex.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.sport.settle.SportBetResultData;
import com.nextgen.gameaggregator.vendor.marblex.dto.CommonDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetDto extends CommonDto implements SportBetResultData {
    @Override
    public String getExternalTransactionId() {
        return null;
    }

    @Override
    public String getVendorBetId() {
        return null;
    }

    @Override
    public String getRoundId() {
        return null;
    }

    @Override
    public String getGameId() {
        return null;
    }

    @Override
    public String getVendorPlayerUsername() {
        return null;
    }

    @Override
    public BigDecimal getBetAmount() {
        return null;
    }

    @Override
    public BigDecimal getNewBetAmount() {
        return null;
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
    public BetStatus getBetStatus() {
        return null;
    }

    @Override
    public Integer getBetType() {
        return null;
    }
}
