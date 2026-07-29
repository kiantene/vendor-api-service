package com.nextgen.gameaggregator.custodianseamless.walletservice.rollback;

import lombok.Data;
import org.springframework.stereotype.Service;

@Service
@Data
public class WalletRollbackContext {
    private String traceId;
    private String referenceId;
    private String username;
    private Long playerId;
    private Integer entityId;
    private Integer tokenId;
}
