package com.nextgen.gameaggregator.core.engine.promo.payout;

import com.nextgen.gameaggregator.core.context.VendorRequestContext;
import com.nextgen.gameaggregator.service.data.model.TxnAmount;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class PayoutTransaction extends VendorRequestContext {
    // --- Vendor's values ---
    protected String vendorTransactionId;
    protected String vendorCampaignCode;
    protected String vendorFreeRoundBonusId;
    protected BigDecimal vendorPayoutAmount;
    protected Long vendorTransactionTime;

    // --- internal values ---
    protected String transactionId;
    protected TxnAmount payout;
}
