package com.nextgen.gameaggregator.vendor.saba.api.settle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.sport.settle.SportBetResultData;
import com.nextgen.gameaggregator.util.DateTimeConversionUtils;
import com.nextgen.gameaggregator.vendor.saba.constant.DateTime;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SettleBetTransactionDto implements SportBetResultData {
    private String userId;
    private String refId;
    private Long txId;
    private String updateTime;
    @JsonProperty("winlostDate")
    private String winLostDate;
    private String status;
    private BigDecimal payout;
    private BigDecimal creditAmount;
    private BigDecimal debitAmount;
    private String extraStatus;
    private String settlementTime;
    private String operationId;

    @Override
    public String getExternalTransactionId() {
        return operationId + "-" + this.getRefId();
    }

    @Override
    public String getVendorBetId() {
        return this.getTxId().toString();
    }

    @Override
    public String getRoundId() {
        return this.getRefId();
    }

    @Override
    public String getGameId() {
        return "saba";
    }

    @Override
    public BigDecimal getBetAmount() {
        return null;
    }

    @Override
    public BigDecimal getWinAmount() {
        return this.getPayout();
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
        try {
            long millisSettlementTime = DateTimeConversionUtils.toUnixTimestamp(
                    this.settlementTime,
                    DateTime.PATTERN_SETTLEMENT_TIME,
                    DateTime.ZONE
            );

            long millisWinLostDate = DateTimeConversionUtils.toUnixTimestamp(
                    this.winLostDate,
                    DateTime.PATTERN_WIN_LOST_DATE,
                    DateTime.ZONE
            );

            return Math.max(millisSettlementTime, millisWinLostDate);

        } catch (Exception ex) {
            //regardless any of above conditions is failed, will fallback to use system generated current time.
            return System.currentTimeMillis();
        }
    }

    @Override
    public BetStatus getBetStatus() {

        BetStatus betStatus = BetStatus.SETTLED;
        if (status != null) {
            if (status.equalsIgnoreCase("refund")
                    || status.equalsIgnoreCase("void")
                    || status.equalsIgnoreCase("reject")) {
                betStatus = BetStatus.REFUNDED;
            }
        }

        return betStatus;
    }

    @Override
    public Integer getBetType() {
        return null;
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
