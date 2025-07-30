package com.nextgen.gameaggregator.core.validator;

import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.exception.*;
import com.nextgen.gameaggregator.core.exception.BetNotAllowedException;
import com.nextgen.gameaggregator.core.exception.DuplicateRequestException;
import com.nextgen.gameaggregator.core.exception.GameTerminatedException;
import com.nextgen.gameaggregator.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.idempotency.DuplicateRequestGuard;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.ValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WalletBetValidator {
    private final DuplicateRequestGuard duplicateRequestGuard;
    private final GameSessionValidator gameSessionValidator;
    private final ValidationService validationService;

    public void validatePreSession(String vendorClassName, BetContext betContext) throws DuplicateRequestException {
        final String BET_ACTION = "bet";
        final String idempotencyKey = betContext.getIdempotencyKey();

        duplicateRequestGuard.ensureNotDuplicate(vendorClassName, BET_ACTION, idempotencyKey);
    }

    /*
    - Valid game session
    - Active game session
    - Active vendor line
    - Active player
    - Currency supported
    - Active game (game level)
    - Active game (house/master agent/agent level)
     */

    public void validateOrThrow(GameSession session, BetContext betContext) throws
            GameSessionExpiredException, GameTerminatedException, PlayerDisabledException, BetNotAllowedException {

        gameSessionValidator.validateOrThrow(session);

        try {
            validationService.isBetAllowed(session, betContext.getVendorPlayerUsername());
        } catch (AuthenticationException authenticationException) {
            throw new GameSessionExpiredException("Session not found or expired");
        } catch (DisabledAgentPlayerException disabledAgentPlayerException) {
            throw new PlayerDisabledException(disabledAgentPlayerException.getMessage());
        } catch (DisabledVendorLineException disabledVendorLineException) {
            throw new BetNotAllowedException(disabledVendorLineException.getMessage(), disabledVendorLineException);
        } catch (DisabledGameException disabledGameException) {
            throw new BetNotAllowedException(disabledGameException.getMessage(), disabledGameException);
        } catch (InvalidPlayerException invalidPlayerException) {
            throw new InvalidRequestException(betContext.getVendorPlayerUsername() + " is not valid");
        } catch (com.nextgen.gameaggregator.exception.GameTerminatedException gameTerminatedException) {
            throw new GameTerminatedException(session.getVendorGameCode() + " game is terminated");
        }
    }
}
