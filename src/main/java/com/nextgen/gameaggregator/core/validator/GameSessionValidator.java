package com.nextgen.gameaggregator.core.validator;

import com.nextgen.gameaggregator.core.exception.GameSessionExpiredException;
import com.nextgen.gameaggregator.core.exception.GameTerminatedException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import org.springframework.stereotype.Service;

@Service
public class GameSessionValidator {
    public void validateOrThrow(GameSession session) throws GameSessionExpiredException, GameTerminatedException {
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
