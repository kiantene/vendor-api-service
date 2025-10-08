package com.nextgen.gameaggregator.core.engine.wallet.bet;

import com.nextgen.gameaggregator.core.exception.BetNotAllowedException;
import com.nextgen.gameaggregator.core.exception.GameSessionExpiredException;
import com.nextgen.gameaggregator.core.exception.GameTerminatedException;
import com.nextgen.gameaggregator.core.exception.PlayerDisabledException;
import com.nextgen.gameaggregator.core.validator.BetValidator;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class WalletBetValidator {
    private final BetValidator validator;

    public void validateRequestContext(BetContext context) {
        // TODO: validate context object
    }

    public void validateBusinessState(GameSession session, BetContext betContext) throws
            GameSessionExpiredException, GameTerminatedException,
            PlayerDisabledException, BetNotAllowedException {

        validator.validateBusinessState(session, betContext.getVendorPlayerUsername(), betContext);
    }
}
