package com.nextgen.gameaggregator.vendor.saba.api.unsettle;

import com.nextgen.gameaggregator.sport.entity.SportUnsettleData;
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

    @Override
    public String getExternalTransactionId() {
        return this.refId;
    }

    @Override
    public String getVendorPlayerUsername() {
        return this.userId;
    }

    @Override
    public Integer getVendorId() {
        return 999;
    }
}