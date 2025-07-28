package com.nextgen.gameaggregator.vendor.aviatorstudio.api.cashin;

import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReverseCashInDto implements RollbackData {

    String previousTransactionId;

    @Override
    public String getRollbackId() {
        return previousTransactionId;
    }

    @Override
    public Long getVendorSettledTime() {
        return System.currentTimeMillis();
    }

    @Override
    public String getRoundId() {
        return null;
    }
}
