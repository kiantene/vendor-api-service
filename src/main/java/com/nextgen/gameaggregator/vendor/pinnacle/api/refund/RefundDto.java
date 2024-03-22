package com.nextgen.gameaggregator.vendor.pinnacle.api.refund;

import com.nextgen.gameaggregator.operator.sport.refund.SportRefundData;
import com.nextgen.gameaggregator.vendor.pinnacle.dto.ActionsWagerInfoDto;

public class RefundDto extends ActionsWagerInfoDto implements SportRefundData {
    @Override
    public String getExternalTransactionId() {
        return this.getWagerId().toString();
    }

    @Override
    public Long getTimestamp() {
        return System.currentTimeMillis();
    }
}
