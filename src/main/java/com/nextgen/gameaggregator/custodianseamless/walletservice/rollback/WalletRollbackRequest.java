package com.nextgen.gameaggregator.custodianseamless.walletservice.rollback;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WalletRollbackRequest {
    private String traceId;
    private String referenceId;
    private String username;
    private Long playerId;
    private Integer entityId;
    private Integer tokenId;
    private Long timestamp;

}
