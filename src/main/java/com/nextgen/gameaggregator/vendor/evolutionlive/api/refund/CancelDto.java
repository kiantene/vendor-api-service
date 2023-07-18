package com.nextgen.gameaggregator.vendor.evolutionlive.api.refund;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.vendor.evolutionlive.dto.DebitCreditCancelDto;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CancelDto extends DebitCreditCancelDto implements RollbackData {

    @Override
    public String getRollbackId() {
        return this.getTransaction().getRefId();
    }

    @Override
    public Long getVendorSettledTime() {
        return null;
    }
}
