package com.nextgen.gameaggregator.core.engine.wallet.bet;

import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.exception.BetNotAllowedException;
import com.nextgen.gameaggregator.core.exception.GameSessionExpiredException;
import com.nextgen.gameaggregator.core.exception.GameTerminatedException;
import com.nextgen.gameaggregator.core.exception.PlayerDisabledException;
import com.nextgen.gameaggregator.core.validator.BetValidator;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.service.business.GameRoundService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
class WalletBetValidator {
    private final BetValidator validator;
    private final GameRoundService gameRoundService;

    public void validateRequestContext(BetContext context) {
        // TODO: validate context object
        if (context.getVendorPlayerUsername() == null) {
            throw new InvalidRequestException("Username cannot be empty");
        }
    }

    public void validateBusinessState(GameSession session, BetContext betContext) throws
            GameSessionExpiredException, GameTerminatedException,
            PlayerDisabledException, BetNotAllowedException {

        validator.validateBusinessState(session, betContext.getVendorPlayerUsername(), betContext);
    }
}
