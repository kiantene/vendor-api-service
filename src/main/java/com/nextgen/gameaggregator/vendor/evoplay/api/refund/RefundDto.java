package com.nextgen.gameaggregator.vendor.evoplay.api.refund;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.vendor.evoplay.dto.CallbackDto;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RefundDto extends CallbackDto implements RollbackData {

    @Override
    public String getRollbackId() {
        return this.getData().getRefund_action_id();
    }

    @Override
    public Long getVendorSettledTime() {
        return null;
    }

    @Override
    public String getRoundId() {
        return null;
    }
}
