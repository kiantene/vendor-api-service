package com.nextgen.gameaggregator.vendor.saba.api.unsettle;

import com.nextgen.gameaggregator.operator.sport.unsettle.SportUnsettleData;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UnsettleTransactionDto implements SportUnsettleData {
    private String userId;
    private String refId;
    private Long txId;
    private String updateTime;
    private BigDecimal creditAmount;
    private BigDecimal debitAmount;
    private String extraStatus;
    private String operationId;

    @Override
    public String getExternalTransactionId() {
        return operationId;
    }

    @Override
    public String getRoundId() {
        return this.refId;
    }

    @Override
    public String getVendorPlayerUsername() {
        return this.userId;
    }

    @Override
    public Long getTimestamp() {
        return System.currentTimeMillis();
    }
}