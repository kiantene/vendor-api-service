package com.nextgen.gameaggregator.entity.ga;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SportRefundBetPatching {
    private String externalTransactionId;
    private String vendorBetId;
    private String vendorPlayerUsername;
    private Long vendorSettleTime;
}
