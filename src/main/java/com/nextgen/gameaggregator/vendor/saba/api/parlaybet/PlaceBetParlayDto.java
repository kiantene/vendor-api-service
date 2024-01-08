package com.nextgen.gameaggregator.vendor.saba.api.parlaybet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.sport.settle.SportBetResultData;
import com.nextgen.gameaggregator.vendor.saba.dto.GeneralDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlaceBetParlayDto extends GeneralDto implements SportBetResultData {
    private String operationId;
    private String userId;
    private Integer currency;
    private String betTime;
    private String updateTime;
    private BigDecimal totalBetAmount;
    private String IP;
    private String tsId;
    private String betFrom;
    private BigDecimal creditAmount;
    private BigDecimal debitAmount;
    private String vendorTransId;
    private List<PlaceBetParlayTxnsDto> txns;
    //    private List<String> ticketDetail;

    private String refId;
    private BigDecimal betAmount;

    @Override
    public String getExternalTransactionId() {
        return this.refId;
    }

    @Override
    public String getVendorBetId() {
        return this.refId;
    }

    @Override
    public String getRoundId() {
        return this.refId;
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
    public BetStatus getBetStatus() {
        return BetStatus.UNSETTLED;
    }

    @Override
    public String getVendorPlayerUsername() {
        return this.userId;
    }

    @Override
    public BigDecimal getNewBetAmount() {
        return null;
    }
}
