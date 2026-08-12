package com.nextgen.gameaggregator.core.validator;

import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.exception.translator.WalletExceptionTranslator;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.service.ValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BetValidatorTest {

    private ValidationService validationService;
    private BetValidator betValidator;

    @BeforeEach
    void setUp() {
        validationService = mock(ValidationService.class);
        WalletExceptionTranslator walletExceptionTranslator = mock(WalletExceptionTranslator.class);
        betValidator = new BetValidator(validationService, walletExceptionTranslator);
    }

    private GameSession launchedSession() {
        GameSession s = new GameSession();
        s.setStatus(1);
        s.setVendorPlayerUsername("player1");
        s.setVendorGameCode("GAME_A");
        s.setVendorGameId(11);
        s.setGameCode("ga-a");
        s.setGameCategoryId(100);
        s.setPlatformId(7);   // exists only on the launched session
        s.setLanguageId(3);   // exists only on the launched session
        s.setCurrencyId(840);
        return s;
    }

    @Test
    void validateBusinessState_gameSwitch_validatesRequestGameOnCopy_withoutMutatingSession() throws Exception {
        GameSession session = launchedSession();
        // Request targets a different (switched) game, already resolved by the enricher.
        BetResultContext context = BetResultContext.builder()
                .vendorGameCode("GAME_B")
                .vendorGameId(22)
                .gameCode("ga-b")
                .gameCategoryId(200)
                .build();

        betValidator.validateBusinessState(session, "player1", context);

        ArgumentCaptor<GameSession> captor = ArgumentCaptor.forClass(GameSession.class);
        verify(validationService).isBetAllowed(captor.capture(), eq("player1"));
        GameSession validated = captor.getValue();

        // Validated view carries the request's game...
        assertNotSame(session, validated);
        assertEquals("GAME_B", validated.getVendorGameCode());
        assertEquals(22, validated.getVendorGameId());
        assertEquals("ga-b", validated.getGameCode());
        assertEquals(200, validated.getGameCategoryId());
        // ...but keeps platform/language/currency from the launched session.
        assertEquals(7, validated.getPlatformId());
        assertEquals(3, validated.getLanguageId());
        assertEquals(840, validated.getCurrencyId());

        // The shared/cached session is never mutated.
        assertEquals("GAME_A", session.getVendorGameCode());
        assertEquals(11, session.getVendorGameId());
        assertEquals("ga-a", session.getGameCode());
        assertEquals(100, session.getGameCategoryId());
    }

    @Test
    void validateBusinessState_sameGameCode_validatesRealSession() throws Exception {
        GameSession session = launchedSession();
        BetResultContext context = BetResultContext.builder()
                .vendorGameCode("GAME_A")
                .vendorGameId(11)
                .gameCode("ga-a")
                .gameCategoryId(100)
                .build();

        betValidator.validateBusinessState(session, "player1", context);

        verify(validationService).isBetAllowed(same(session), eq("player1"));
    }

    @Test
    void validateBusinessState_nullRequestGameCode_validatesRealSession() throws Exception {
        GameSession session = launchedSession();
        BetResultContext context = BetResultContext.builder().build(); // no vendorGameCode

        betValidator.validateBusinessState(session, "player1", context);

        verify(validationService).isBetAllowed(same(session), eq("player1"));
    }

    @Test
    void validateBusinessState_switchButUnresolvedGame_fallsBackToRealSession() throws Exception {
        GameSession session = launchedSession();
        // Switched game code, but the enricher did not resolve it (vendorGameId null).
        // Guard must NOT overlay nulls onto the view — fall back to the launched session.
        BetResultContext context = BetResultContext.builder()
                .vendorGameCode("GAME_B")
                .build(); // vendorGameId/gameCode/gameCategoryId all null

        betValidator.validateBusinessState(session, "player1", context);

        verify(validationService).isBetAllowed(same(session), eq("player1"));
    }
}
