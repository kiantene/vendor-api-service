package com.nextgen.gameaggregator.vendor.dblive.api.activitypayout;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActivityPayoutRollbackDto extends ActivityPayoutCommonDto implements RollbackData {

    @Override
    public String getRollbackId() {
        return String.valueOf(this.getTransferNo());
    }

    @Override
    public Long getVendorSettledTime() {
        return this.getPayoutTime();
    }

    @Override
    public String getRoundId() {
        return "";
    }
}
