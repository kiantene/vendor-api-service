package com.nextgen.gameaggregator.core.validator;

import com.nextgen.gameaggregator.core.exception.BetNotAllowedException;
import com.nextgen.gameaggregator.core.exception.GameSessionExpiredException;
import com.nextgen.gameaggregator.core.exception.GameTerminatedException;
import com.nextgen.gameaggregator.core.exception.PlayerDisabledException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.ValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BetValidator {
    private final ValidationService validationService;

    /**
     * Valid game session
     * Active game session
     * Active vendor line
     * Active player
     * Currency supported
     * Active game (game level)
     * Active game (house/master agent/agent level)
     */
    public void validateBusinessState(GameSession session, String vendorPlayerUsername) throws
            GameSessionExpiredException, GameTerminatedException,
            PlayerDisabledException, BetNotAllowedException {

        validateSession(session);

        try {
            validationService.isBetAllowed(session, vendorPlayerUsername);
        } catch (AuthenticationException authenticationException) {

            throw new GameSessionExpiredException("Session not found or expired");
        } catch (DisabledAgentPlayerException disabledAgentPlayerException) {

            throw new PlayerDisabledException(disabledAgentPlayerException.getMessage());
        } catch (DisabledVendorLineException disabledVendorLineException) {

            throw new BetNotAllowedException(disabledVendorLineException.getMessage(), disabledVendorLineException);
        } catch (DisabledGameException disabledGameException) {

            throw new BetNotAllowedException(disabledGameException.getMessage(), disabledGameException);
        } catch (InvalidPlayerException invalidPlayerException) {

            throw new com.nextgen.gameaggregator.core.exception.InvalidRequestException(vendorPlayerUsername + " is not valid");
        } catch (com.nextgen.gameaggregator.exception.GameTerminatedException gameTerminatedException) {

            throw new GameTerminatedException(session.getVendorGameCode() + " game is terminated");
        }
    }

    private void validateSession(GameSession session) throws GameSessionExpiredException, GameTerminatedException {
        if (session == null) {
            throw new GameSessionExpiredException("Session not found or expired");
        }
        if (isTerminated(session)) {
            throw new GameTerminatedException(session.getVendorGameCode() + " game is terminated");
        }
    }

    private boolean isTerminated(GameSession session) {
        return session.getStatus() == 0;
    }
}
