package com.nextgen.gameaggregator.core.validator;

import com.nextgen.gameaggregator.core.context.VendorGameAware;
import com.nextgen.gameaggregator.core.context.VendorRequestContext;
import com.nextgen.gameaggregator.core.exception.GameSessionExpiredException;
import com.nextgen.gameaggregator.core.exception.GameTerminatedException;
import com.nextgen.gameaggregator.core.exception.translator.WalletExceptionTranslator;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.service.ValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
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
    public void validateBusinessState(GameSession session, String vendorPlayerUsername, VendorRequestContext context) {

        validateSession(session, context);
        try {
            validationService.isBetAllowed(buildValidationView(session, context), vendorPlayerUsername);
        } catch (Exception ex) {
            throw walletExceptionTranslator.translate(ex, context);
        }
    }

    /**
     * Supports Vendor Lobby ("More Games") game switches: the request may target a
     * different game than the launched session. When it does, validate against the
     * request's game — but on a short-lived copy, never the shared/cached session.
     * <p>
     * platformId/languageId are kept from the real session (they only exist there);
     * the request's already-resolved game fields (vendorGameCode/vendorGameId/gameCode/
     * gameCategoryId, produced by BaseEnricher#enrichVendorGame — which has already
     * confirmed the game exists and is enabled) are overlaid onto the copy.
     * <p>
     * Not mutating the real session avoids a race when a player runs two games
     * concurrently on one session. When there is no switch (same or missing game code),
     * the real session is returned unchanged.
     */
    private GameSession buildValidationView(GameSession session, VendorRequestContext context) {
        if (!(context instanceof VendorGameAware game)) {
            return session;
        }
        String requestGameCode = context.getVendorGameCode();
        if (requestGameCode == null || requestGameCode.equals(session.getVendorGameCode())) {
            return session;
        }

        // The overlay relies on BaseEnricher#enrichVendorGame having resolved the request
        // game. In every real call path it runs before validation, but guard anyway: if the
        // request game is unresolved (vendorGameId null), overlaying nulls would poison
        // ValidationService.isBetAllowed (game-category/vendor-game-id lookups). Fall back to
        // validating the launched session rather than a null-poisoned view.
        if (game.getVendorGameId() == null) {
            log.warn("[GAME_SWITCH] Request game unresolved (vendorGameId null); validating launched session. TraceId: {}, Request: {}",
                    context.getTraceId(), requestGameCode);
            return session;
        }

        log.info("[GAME_SWITCH] Validating request game over launched session. TraceId: {}, Session: {}, Request: {}",
                context.getTraceId(), session.getVendorGameCode(), requestGameCode);

        GameSession view = new GameSession(session);
        view.setVendorGameCode(requestGameCode);
        view.setVendorGameId(game.getVendorGameId());
        view.setGameCode(game.getGameCode());
        view.setGameCategoryId(game.getGameCategoryId());
        return view;
    }

    public void validateSession(GameSession session, VendorRequestContext context) throws GameSessionExpiredException, GameTerminatedException {
        if (session == null) {
            throw new GameSessionExpiredException(context, "Session not found or expired");
        }
        if (isTerminated(session)) {
            throw new GameTerminatedException(context, session.getVendorGameCode() + " game is terminated");
        }
    }

    private boolean isTerminated(GameSession session) {
        return session.getStatus() == 0;
    }
}
