package com.nextgen.gameaggregator.custodianseamless.walletservice.rollback;

import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class WalletRollbackRequestMapper {

    public WalletRollbackRequest toWalletRollbackRequest(WalletRollbackContext context) {
        if (context == null) {
            return null;
        }

        return WalletRollbackRequest.builder()
                .traceId(context.getTraceId())
                .referenceId(context.getReferenceId())
                .username(context.getUsername())
                .playerId(context.getPlayerId())
                .entityId(context.getEntityId())
                .tokenId(context.getTokenId())
                .timestamp(Instant.now().toEpochMilli())
                .build();
    }


}
