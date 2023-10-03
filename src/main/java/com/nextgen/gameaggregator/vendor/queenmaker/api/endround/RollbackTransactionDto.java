package com.nextgen.gameaggregator.vendor.queenmaker.api.endround;

import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.vendor.queenmaker.service.VendorService;

public class RollbackTransactionDto extends CreditTransactionsDto implements RollbackData {
    @Override
    public String getRollbackId() {
        return this.getRefptxid();
    }

    @Override
    public Long getVendorSettledTime() {
        return VendorService.convertToTimestamp(this.getTimestamp());
    }
}
