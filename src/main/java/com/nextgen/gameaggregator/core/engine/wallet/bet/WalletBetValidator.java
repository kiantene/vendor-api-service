package com.nextgen.gameaggregator.core.engine.wallet.bet;

import com.nextgen.gameaggregator.core.exception.*;
import com.nextgen.gameaggregator.core.idempotency.DuplicateRequestGuard;
import com.nextgen.gameaggregator.core.validator.BetValidator;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WalletBetValidator {
    private final DuplicateRequestGuard duplicateRequestGuard;
    private final BetValidator validator;

    public void validateRequestContext(String vendorClassName, BetContext context) throws DuplicateRequestException {
        final String ACTION = "bet";
        final String idempotencyKey = context.getIdempotencyKey();

        // TODO: validate context object

        duplicateRequestGuard.ensureNotDuplicate(vendorClassName, ACTION, idempotencyKey);
    }

    public void clearRequestIdempotent() {
        duplicateRequestGuard.clear();
    }

    public void validateBusinessState(GameSession session, BetContext betContext) throws
            GameSessionExpiredException, GameTerminatedException,
            PlayerDisabledException, BetNotAllowedException {

        validator.validateBusinessState(session, betContext.getVendorPlayerUsername());
    }
}
