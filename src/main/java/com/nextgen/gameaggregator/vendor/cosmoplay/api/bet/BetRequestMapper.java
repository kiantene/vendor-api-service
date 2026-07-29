package com.nextgen.gameaggregator.vendor.cosmoplay.api.bet;

import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContextMapper;
import com.nextgen.gameaggregator.vendor.cosmoplay.entity.Player;
import com.nextgen.gameaggregator.vendor.cosmoplay.util.Amount;
import org.springframework.stereotype.Component;

@Component
public class BetRequestMapper implements BetContextMapper<BetRequest> {
    @Override
    public BetContext toInternal(BetRequest request) {
        Player player = Player.of(request.getPlayerID());

        if (Boolean.TRUE.equals(player.hasError())) {
            throw new InvalidRequestException(player.getError());
        }

        return BetContext.builder()
                .idempotencyKey(request.getSpinID())
                .vendorBetId(request.getSpinID())
                .roundId(request.getRoundID())
                .vendorPlayerUsername(player.getId())
                .vendorGameCode(request.getGameID())
                .betAmount(Amount.internal(request.getBetAmount()))
                .build();
    }
}
