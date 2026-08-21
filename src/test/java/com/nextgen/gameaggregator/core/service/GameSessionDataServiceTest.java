package com.nextgen.gameaggregator.core.service;

import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.exception.GameSessionExpiredException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.repository.ga.writer.RawGameSessionRepository;
import com.nextgen.gameaggregator.service.GameSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GameSessionDataServiceTest {

    private RawGameSessionRepository repository;
    private GameSessionDataService dataService;

    @BeforeEach
    void setUp() {
        GameSessionService gameSessionService = mock(GameSessionService.class);
        repository = mock(RawGameSessionRepository.class);
        dataService = new GameSessionDataService(gameSessionService, repository);
    }

    private GameSession session(String vendorGameCode, long createTime) {
        GameSession s = new GameSession();
        s.setVendorPlayerUsername("player1");
        s.setVendorGameCode(vendorGameCode);
        s.setPlatformId(7);
        s.setLanguageId(3);
        s.setCreateTime(createTime);
        s.setStatus(1);
        return s;
    }

    private BetResultContext contextForGame(String vendorGameCode) {
        return BetResultContext.builder().vendorGameCode(vendorGameCode).build();
    }

    @Test
    void getByVendorPlayerUsername_returnsLatestSession_ignoringSwitchedGameCode() {
        // Player launched GAME_A, then switched to GAME_B via the Vendor Lobby.
        // The latest launched session (GAME_A) is what the top-1 finder returns; the
        // request carries GAME_B. The real (latest) session must still be returned, with
        // platformId/languageId intact — the method must NOT re-filter by request game code.
        GameSession launched = session("GAME_A", 200L);
        when(repository.findTop1ByVendorPlayerUsernameOrderByCreateTimeDesc("player1")).thenReturn(launched);

        GameSession result = dataService.getByVendorPlayerUsername("player1", contextForGame("GAME_B"));

        assertSame(launched, result);
        assertEquals("GAME_A", result.getVendorGameCode());
        assertEquals(7, result.getPlatformId());
        assertEquals(3, result.getLanguageId());
    }

    @Test
    void getByVendorPlayerUsername_usesOrderedTop1Finder_notFetchAll() {
        // The latest-by-createTime selection is pushed to the DB (ORDER BY createTime DESC
        // LIMIT 1). Guard against regressing to the fetch-all-then-reduce-in-JVM pattern.
        GameSession latest = session("GAME_A", 300L);
        when(repository.findTop1ByVendorPlayerUsernameOrderByCreateTimeDesc("player1")).thenReturn(latest);

        GameSession result = dataService.getByVendorPlayerUsername("player1", contextForGame("GAME_A"));

        assertSame(latest, result);
        verify(repository).findTop1ByVendorPlayerUsernameOrderByCreateTimeDesc("player1");
        verify(repository, never()).findByVendorPlayerUsername(anyString());
    }

    @Test
    void getByVendorPlayerUsername_throwsWhenNoSession() {
        when(repository.findTop1ByVendorPlayerUsernameOrderByCreateTimeDesc("player1")).thenReturn(null);

        assertThrows(GameSessionExpiredException.class,
                () -> dataService.getByVendorPlayerUsername("player1", contextForGame("GAME_A")));
    }
}
