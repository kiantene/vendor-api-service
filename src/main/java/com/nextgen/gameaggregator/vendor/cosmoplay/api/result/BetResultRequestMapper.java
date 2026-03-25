package com.nextgen.gameaggregator.vendor.cosmoplay.api.result;

import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContextMapper;
import com.nextgen.gameaggregator.vendor.cosmoplay.entity.Player;
import com.nextgen.gameaggregator.vendor.cosmoplay.util.Amount;
import org.springframework.stereotype.Component;

@Component
public class BetResultRequestMapper implements BetResultContextMapper<BetResultRequest> {

    @Override
    public BetResultContext toInternal(BetResultRequest request) {
        Player player = Player.of(request.getPlayerID());

        if (player.hasError()) {
            throw new InvalidRequestException(player.getError());
        }

        return BetResultContext.builder()
                .idempotencyKey(request.getSpinID())
                .vendorBetId(request.getSpinID())
                .roundId(request.getRoundID())
                .vendorPlayerUsername(player.getId())
                .vendorGameCode(request.getGameID())
                .winAmount(Amount.internal(request.getWinAmount()))
                .roundEnded(request.getIsRoundEnd())
                .build();
    }
}
