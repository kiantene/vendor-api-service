package com.nextgen.gameaggregator.vendor.saba.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.sport.entity.SportBetResultData;
import com.nextgen.gameaggregator.vendor.saba.dto.GeneralDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlaceBetDto extends GeneralDto implements SportBetResultData {

    private String operationId;
    private String userId;
    private String betTime;
    private BigDecimal betAmount;
    private BigDecimal actualAmount;
    private Integer oddsType;
    private Integer oddsId;
    private BigDecimal odds;
    private String updateTime;
    private String IP;
    private Boolean isLive;
    private String refId;
    private String tsId;
    private BigDecimal creditAmount;
    private BigDecimal debitAmount;
    private String vendorTransId;

    @Override
    public String getExternalTransactionId() {
        return this.refId;
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
    public BigDecimal getBetAmount() {
        return this.betAmount;
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
        return BetStatus.UNSETTLED;
    }

    @Override
    public BigDecimal getActualBetAmount() {
        return this.actualAmount;
    }

    @Override
    public BigDecimal getOdds() {
        return this.odds;
    }

    @Override
    public Integer getOddTypeId() {
        return this.oddsType;
    }
}
