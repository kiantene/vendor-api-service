package com.nextgen.gameaggregator.vendor.bgaming.api.rollback;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RollbackDto implements RollbackData {
    private String betId;
    private Long timestamp;

    @Override
    public String getRollbackId() {
        return this.betId;
    }

    @Override
    public Long getVendorSettledTime() {
        return this.timestamp;
    }
}
