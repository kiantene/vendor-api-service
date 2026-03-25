package com.nextgen.gameaggregator.vendor.cosmoplay.api.balance;

import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateContext;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateContextMapper;
import com.nextgen.gameaggregator.vendor.cosmoplay.entity.Player;
import org.springframework.stereotype.Component;

@Component
public class BalanceRequestMapper implements AuthenticateContextMapper<BalanceRequest> {
    @Override
    public AuthenticateContext toInternal(BalanceRequest request) {
        Player player = Player.of(request.getPlayerID());

        if (player.hasError()) {
            throw new InvalidRequestException(player.getError());
        }

        return AuthenticateContext.builder()
                .vendorPlayerUsername(player.getId())
                .vendorGameCode(request.getGameID())
                .token(request.getAuthToken())
                .build();
    }
}
