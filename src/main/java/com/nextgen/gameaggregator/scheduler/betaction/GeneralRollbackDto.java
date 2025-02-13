package com.nextgen.gameaggregator.scheduler.betaction;

import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import lombok.Data;

@Data
public class GeneralRollbackDto implements RollbackData {

    private String rollbackId;
    private Long vendorSettledTime;
    private String roundId;

    @Override
    public String getRollbackId() {
        return this.rollbackId;
    }

    @Override
    public Long getVendorSettledTime() {
        return this.vendorSettledTime;
    }

    @Override
    public String getRoundId() {
        return this.roundId;
    }
}
