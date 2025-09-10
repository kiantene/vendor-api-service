package com.nextgen.gameaggregator.core.validator;

import com.nextgen.gameaggregator.core.exception.GameSessionExpiredException;
import com.nextgen.gameaggregator.core.exception.GameTerminatedException;
import com.nextgen.gameaggregator.core.exception.translator.WalletExceptionTranslator;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.service.ValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BetValidator {
    private final ValidationService validationService;
    private final WalletExceptionTranslator walletExceptionTranslator;

    /**
     * Valid game session
     * Active game session
     * Active vendor line
     * Active player
     * Currency supported
     * Active game (game level)
     * Active game (house/master agent/agent level)
     */
    public void validateBusinessState(GameSession session, String vendorPlayerUsername) {

        validateSession(session);
        try {
            validationService.isBetAllowed(session, vendorPlayerUsername);
        } catch (Exception ex) {
            throw walletExceptionTranslator.translate(ex);
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
