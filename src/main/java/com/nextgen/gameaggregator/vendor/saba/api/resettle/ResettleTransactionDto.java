package com.nextgen.gameaggregator.vendor.saba.api.resettle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.sport.resettle.SportResettleData;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResettleTransactionDto implements SportResettleData {
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
        return this.txId.toString();
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
        return null;
    }

    @Override
    public BigDecimal getWinAmount() {
        return this.payout;
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
        // Parse the original string to a ZonedDateTime
        ZonedDateTime unixWinLoseDate = ZonedDateTime.parse(this.winLostDate, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        LocalDateTime unixSettlementTime = LocalDateTime.parse(this.settlementTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        //convert to millisecond
        long millisWinLostDate = unixWinLoseDate.toInstant().toEpochMilli();
        long millisSettlementTime = unixSettlementTime.toInstant(unixWinLoseDate.getOffset()).toEpochMilli();

        //compare and get the Later Time
        return Math.max(millisWinLostDate, millisSettlementTime);
    }

    @Override
    public BetStatus getBetStatus() {
        return BetStatus.SETTLED;
    }

    @Override
    public BigDecimal getNewWinAmount() {
        return this.payout;
    }

    @Override
    public String getVendorPlayerUsername() {
        return this.userId;
    }
}
