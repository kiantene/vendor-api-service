package com.nextgen.gameaggregator.vendor.cosmoplay.api.rollback;

import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContextMapper;
import com.nextgen.gameaggregator.vendor.cosmoplay.entity.Player;
import org.springframework.stereotype.Component;

@Component
public class RollbackRequestMapper implements BetRollbackContextMapper<RollbackRequest> {
    @Override
    public BetRollbackContext toInternal(RollbackRequest request) {
        Player player = Player.of(request.getPlayerID());

        if (player.hasError()) {
            throw new InvalidRequestException(player.getError());
        }

        return BetRollbackContext.builder()
                .idempotencyKey(request.getSpinID())
                .vendorBetId(request.getSpinID())
                .roundId(request.getRoundID())
                .vendorGameCode(request.getGameID())
                .vendorPlayerUsername(player.getId())
                .build();
    }
}
