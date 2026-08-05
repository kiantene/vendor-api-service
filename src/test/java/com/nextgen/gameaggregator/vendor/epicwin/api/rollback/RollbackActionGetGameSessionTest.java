package com.nextgen.gameaggregator.vendor.epicwin.api.rollback;

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
 * Unit tests for {@code RollbackAction.getGameSession(RollbackDto, String)}.
 * <p>
 * Mirrors the fallback logic added to {@code SettleAction}: when the initial
 * session lookup throws {@link AuthenticationException}, a new session
 * should be regenerated using the same {@code gameCode} value for both the
 * lookup attempt and the rebuild (unlike SettleAction/GameResult, RollbackDto
 * only exposes a single {@code getGameCode()} field, no separate
 * {@code getGameId()}).
 */
@ExtendWith(MockitoExtension.class)
class RollbackActionGetGameSessionTest {

    private static final String PLAYER_ID = "355ene4lckf";
    private static final String GAME_CODE = "48002";
    private static final String TRACE_ID = "trace-id-456";

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
    private RollbackAction rollbackAction;

    private Method getGameSessionMethod;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        getGameSessionMethod = RollbackAction.class.getDeclaredMethod("getGameSession", RollbackDto.class, String.class);
        getGameSessionMethod.setAccessible(true);
    }

    private GameSession invokeGetGameSession(RollbackDto dto) throws Throwable {
        try {
            return (GameSession) getGameSessionMethod.invoke(rollbackAction, dto, TRACE_ID);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    private RollbackDto buildRollbackDto() {
        RollbackDto dto = new RollbackDto();
        dto.setPlayerId(PLAYER_ID);
        dto.setGameCode(GAME_CODE);
        return dto;
    }

    // ------------------------------------------------------------------
    // Happy path: session found on first lookup -> no fallback triggered
    // ------------------------------------------------------------------
    @Test
    void whenSessionFound_returnsExistingSession_andDoesNotTriggerFallback() throws Throwable {
        RollbackDto dto = buildRollbackDto();
        GameSession existingSession = new GameSession();
        existingSession.setVendorPlayerUsername(PLAYER_ID);
        existingSession.setVendorGameCode(GAME_CODE);

        when(gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(PLAYER_ID, GAME_CODE))
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
        RollbackDto dto = buildRollbackDto();
        GameSession regeneratedSession = new GameSession();
        regeneratedSession.setVendorPlayerUsername(PLAYER_ID);

        when(gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(PLAYER_ID, GAME_CODE))
                .thenThrow(new AuthenticationException());
        when(gameSessionService.generateNewSessionToken(PLAYER_ID))
                .thenReturn(regeneratedSession);

        GameSession result = invokeGetGameSession(dto);

        assertThat(result).isSameAs(regeneratedSession);
        assertThat(result.getToken()).isEqualTo(TRACE_ID);
        assertThat(result.getVendorToken()).isEqualTo(TRACE_ID);

        InOrder inOrder = inOrder(gameSessionService);
        inOrder.verify(gameSessionService).generateNewSessionToken(PLAYER_ID);
        inOrder.verify(gameSessionService).updateByVendorGameCode(regeneratedSession, GAME_CODE);
        inOrder.verify(gameSessionService).updateByVendorCurrencyId(regeneratedSession);
    }

    @Test
    void fallbackReusesSameGameCode_forBothLookupAndRebuild() throws Throwable {
        // Guard against a future refactor accidentally passing a different
        // value to updateByVendorGameCode(...) than what was used for the
        // initial lookup - both calls should always use the same gameCode
        // value from the incoming RollbackDto.
        RollbackDto dto = new RollbackDto();
        dto.setPlayerId(PLAYER_ID);
        dto.setGameCode("shared-code-value");

        GameSession regeneratedSession = new GameSession();

        when(gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(PLAYER_ID, "shared-code-value"))
                .thenThrow(new AuthenticationException());
        when(gameSessionService.generateNewSessionToken(PLAYER_ID)).thenReturn(regeneratedSession);

        invokeGetGameSession(dto);

        verify(gameSessionService).updateByVendorGameCode(regeneratedSession, "shared-code-value");
    }

    // ------------------------------------------------------------------
    // Exception propagation from within the fallback branch
    // ------------------------------------------------------------------
    @Test
    void whenGenerateNewSessionTokenFails_propagatesInvalidPlayerException() throws Throwable {
        RollbackDto dto = buildRollbackDto();

        when(gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(PLAYER_ID, GAME_CODE))
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
        RollbackDto dto = buildRollbackDto();
        GameSession regeneratedSession = new GameSession();

        when(gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(PLAYER_ID, GAME_CODE))
                .thenThrow(new AuthenticationException());
        when(gameSessionService.generateNewSessionToken(PLAYER_ID))
                .thenReturn(regeneratedSession);
        doThrow(new GameNotSupportedException())
                .when(gameSessionService).updateByVendorGameCode(regeneratedSession, GAME_CODE);

        assertThatThrownBy(() -> invokeGetGameSession(dto))
                .isInstanceOf(GameNotSupportedException.class);

        verify(gameSessionService, never()).updateByVendorCurrencyId(any());
    }

    @Test
    void whenUpdateByVendorCurrencyIdFails_propagatesVendorCurrencyNotSupportException() throws Throwable {
        RollbackDto dto = buildRollbackDto();
        GameSession regeneratedSession = new GameSession();

        when(gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(PLAYER_ID, GAME_CODE))
                .thenThrow(new AuthenticationException());
        when(gameSessionService.generateNewSessionToken(PLAYER_ID))
                .thenReturn(regeneratedSession);
        doThrow(new VendorCurrencyNotSupportException())
                .when(gameSessionService).updateByVendorCurrencyId(regeneratedSession);

        assertThatThrownBy(() -> invokeGetGameSession(dto))
                .isInstanceOf(VendorCurrencyNotSupportException.class);
    }
}