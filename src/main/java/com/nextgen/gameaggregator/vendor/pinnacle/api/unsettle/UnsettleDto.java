package com.nextgen.gameaggregator.vendor.pinnacle.api.unsettle;

import com.nextgen.gameaggregator.operator.sport.unsettle.SportUnsettleData;
import com.nextgen.gameaggregator.vendor.pinnacle.dto.ActionsWagerInfoDto;

public class UnsettleDto extends ActionsWagerInfoDto implements SportUnsettleData {
    @Override
    public String getExternalTransactionId() {
        return this.getWagerId().toString();
    }

    @Override
    public Long getTimestamp() {
        return System.currentTimeMillis();
    }
}
