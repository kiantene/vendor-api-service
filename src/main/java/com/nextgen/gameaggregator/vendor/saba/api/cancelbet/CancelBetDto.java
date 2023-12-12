package com.nextgen.gameaggregator.vendor.saba.api.cancelbet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.sport.entity.SportBetResultData;
import com.nextgen.gameaggregator.vendor.saba.dto.GeneralDto;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CancelBetDto extends GeneralDto implements SportBetResultData {
    private String action;
    private String operationId;
    private String userId;
    private String updateTime;
    private List<CancelBetTxnsDto> txns;

    private String refId;

    @Override
    public String getExternalTransactionId() {
        return this.getRefId();
    }

    @Override
    public String getVendorBetId() {
        return this.getRefId();
    }

    @Override
    public String getRoundId() {
        return this.getRefId();
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
        return System.currentTimeMillis();
    }

    @Override
    public Long getVendorSettleTime() {
        return System.currentTimeMillis();
    }

    @Override
    public BigDecimal getJackpotAmount() {
        return BigDecimal.ZERO;
    }

    @Override
    public Integer getIsFreespin() {
        return 0;
    }

    @Override
    public BetStatus getBetStatus() {
        return BetStatus.CANCELLED;
    }

    @Override
    public String getVendorPlayerUsername() {
        return this.getUserId();
    }

    @Override
    public BigDecimal getNewBetAmount() {
        return null;
    }
}
