package com.nextgen.gameaggregator.vendor.epicwin.api.endround;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.GameNotSupportedException;
import com.nextgen.gameaggregator.exception.InvalidPlayerException;
import com.nextgen.gameaggregator.exception.VendorCurrencyNotSupportException;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.vendor.epicwin.service.VendorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@code SettleAction.getGameSession(SettleDto, String)}.
 * <p>
 * This method was recently updated to add an Advantplay-style fallback:
 * when the initial session lookup throws {@link AuthenticationException}
 * (e.g. because the original session has expired for a delayed GameResult
 * callback), a new session should be regenerated instead of failing the
 * whole request.
 * <p>
 * Since {@code getGameSession} is a private method, we invoke it via
 * reflection to keep the test focused and avoid having to stub out the
 * entire {@code settle()} flow (signature verification, wallet calls, etc.).
 */
@ExtendWith(MockitoExtension.class)
class SettleActionGetGameSessionTest {

    private static final String PLAYER_ID = "355ene4lckf";
    private static final String GAME_ID = "48002";
    private static final String GAME_CODE = "48002";
    private static final String TRACE_ID = "trace-id-123";

    @Mock
    private HttpService httpService;
    @Mock
    private GameSessionService gameSessionService;
    @Mock
    private WalletService walletService;
    @Mock
    private VendorService vendorService;
    @Mock
    private VendorLineService vendorLineService;

    @InjectMocks
    private SettleAction settleAction;

    private Method getGameSessionMethod;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        getGameSessionMethod = SettleAction.class.getDeclaredMethod("getGameSession", SettleDto.class, String.class);
        getGameSessionMethod.setAccessible(true);
    }

    private GameSession invokeGetGameSession(SettleDto dto) throws Throwable {
        try {
            return (GameSession) getGameSessionMethod.invoke(settleAction, dto, TRACE_ID);
        } catch (InvocationTargetException e) {
            // unwrap so tests can assert on the real checked exception
            throw e.getCause();
        }
    }

    private SettleDto buildSettleDto() {
        // NOTE: SettleDto has a single `gameCode` field. getGameId() (required by
        // the BetResultData interface) is implemented as `return this.gameCode`,
        // so there is no independent gameId value/setter - getGameId() and
        // getGameCode() are always the same value for a given SettleDto instance.
        SettleDto dto = new SettleDto();
        dto.setPlayerId(PLAYER_ID);
        dto.setGameCode(GAME_CODE);
        return dto;
    }

    // ------------------------------------------------------------------
    // Happy path: session found on first lookup -> no fallback triggered
    // ------------------------------------------------------------------
    @Test
    void whenSessionFound_returnsExistingSession_andDoesNotTriggerFallback() throws Throwable {
        SettleDto dto = buildSettleDto();
        GameSession existingSession = new GameSession();
        existingSession.setVendorPlayerUsername(PLAYER_ID);
        existingSession.setVendorGameCode(GAME_ID);

        when(gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(PLAYER_ID, GAME_ID))
                .thenReturn(existingSession);

        GameSession result = invokeGetGameSession(dto);

        assertThat(result).isSameAs(existingSession);
        verify(gameSessionService, never()).generateNewSessionToken(anyString());
        verify(gameSessionService, never()).updateByVendorGameCode(any(), anyString());
        verify(gameSessionService, never()).updateByVendorCurrencyId(any());
    }

    // ------------------------------------------------------------------
    // Fallback path: AuthenticationException -> session regenerated
    // ------------------------------------------------------------------
    @Test
    void whenSessionNotFound_regeneratesSession_andSetsTraceIdAsTokens() throws Throwable {
        SettleDto dto = buildSettleDto();
        GameSession regeneratedSession = new GameSession();
        regeneratedSession.setVendorPlayerUsername(PLAYER_ID);

        when(gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(PLAYER_ID, GAME_ID))
                .thenThrow(new AuthenticationException());
        when(gameSessionService.generateNewSessionToken(PLAYER_ID))
                .thenReturn(regeneratedSession);

        GameSession result = invokeGetGameSession(dto);

        assertThat(result).isSameAs(regeneratedSession);
        assertThat(result.getToken()).isEqualTo(TRACE_ID);
        assertThat(result.getVendorToken()).isEqualTo(TRACE_ID);

        // verify fallback call order: generate -> updateByVendorGameCode -> updateByVendorCurrencyId
        InOrder inOrder = inOrder(gameSessionService);
        inOrder.verify(gameSessionService).generateNewSessionToken(PLAYER_ID);
        inOrder.verify(gameSessionService).updateByVendorGameCode(regeneratedSession, GAME_CODE);
        inOrder.verify(gameSessionService).updateByVendorCurrencyId(regeneratedSession);
    }

    // NOTE: An earlier version of this test suite included a regression guard
    // asserting that getGameId() and getGameCode() must not be swapped. That
    // assumed they were two independent fields on SettleDto. In reality,
    // getGameId() is implemented as `return this.gameCode`, so both getters
    // always return the exact same value - there is nothing to swap, and the
    // main `whenSessionNotFound_regeneratesSession_...` test above already
    // covers that the fallback passes `gameCode` through to
    // updateByVendorGameCode(...) correctly.

    // ------------------------------------------------------------------
    // Exception propagation from within the fallback branch
    // ------------------------------------------------------------------
    @Test
    void whenGenerateNewSessionTokenFails_propagatesInvalidPlayerException() throws Throwable {
        SettleDto dto = buildSettleDto();

        when(gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(PLAYER_ID, GAME_ID))
                .thenThrow(new AuthenticationException());
        when(gameSessionService.generateNewSessionToken(PLAYER_ID))
                .thenThrow(new InvalidPlayerException());

        assertThatThrownBy(() -> invokeGetGameSession(dto))
                .isInstanceOf(InvalidPlayerException.class);

        verify(gameSessionService, never()).updateByVendorGameCode(any(), anyString());
        verify(gameSessionService, never()).updateByVendorCurrencyId(any());
    }

    @Test
    void whenUpdateByVendorGameCodeFails_propagatesGameNotSupportedException() throws Throwable {
        SettleDto dto = buildSettleDto();
        GameSession regeneratedSession = new GameSession();

        when(gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(PLAYER_ID, GAME_ID))
                .thenThrow(new AuthenticationException());
        when(gameSessionService.generateNewSessionToken(PLAYER_ID))
                .thenReturn(regeneratedSession);
        doThrow(new GameNotSupportedException())
                .when(gameSessionService).updateByVendorGameCode(regeneratedSession, GAME_CODE);

        assertThatThrownBy(() -> invokeGetGameSession(dto))
                .isInstanceOf(GameNotSupportedException.class);

        // currency update should never be reached if game code mapping fails
        verify(gameSessionService, never()).updateByVendorCurrencyId(any());
    }

    @Test
    void whenUpdateByVendorCurrencyIdFails_propagatesVendorCurrencyNotSupportException() throws Throwable {
        SettleDto dto = buildSettleDto();
        GameSession regeneratedSession = new GameSession();

        when(gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(PLAYER_ID, GAME_ID))
                .thenThrow(new AuthenticationException());
        when(gameSessionService.generateNewSessionToken(PLAYER_ID))
                .thenReturn(regeneratedSession);
        doThrow(new VendorCurrencyNotSupportException())
                .when(gameSessionService).updateByVendorCurrencyId(regeneratedSession);

        assertThatThrownBy(() -> invokeGetGameSession(dto))
                .isInstanceOf(VendorCurrencyNotSupportException.class);
    }
}