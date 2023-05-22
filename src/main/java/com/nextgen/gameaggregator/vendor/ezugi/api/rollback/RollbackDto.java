package com.nextgen.gameaggregator.vendor.ezugi.api.rollback;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.vendor.ezugi.dto.CommonDto;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RollbackDto extends CommonDto implements RollbackData {
    private String uid;
    private String transactionId;
    private String roundId;
    private String currency;

    @Override
    public String getRollbackId() {
        return this.roundId;
    }

    @Override
    public Long getVendorSettledTime() {
        return this.getTimestamp();
    }
}
