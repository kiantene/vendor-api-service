package com.nextgen.gameaggregator.core.service;

import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.exception.GameSessionExpiredException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.repository.ga.writer.RawGameSessionRepository;
import com.nextgen.gameaggregator.service.GameSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

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
        // The latest launched session is GAME_A (higher createTime); the request
        // carries GAME_B. The real (latest) session must still be returned, with
        // platformId/languageId intact — NOT filtered out into a lossy rebuild.
        GameSession launched = session("GAME_A", 200L);
        GameSession older = session("GAME_C", 100L);
        when(repository.findByVendorPlayerUsername("player1")).thenReturn(List.of(older, launched));

        GameSession result = dataService.getByVendorPlayerUsername("player1", contextForGame("GAME_B"));

        assertSame(launched, result);
        assertEquals("GAME_A", result.getVendorGameCode());
        assertEquals(7, result.getPlatformId());
        assertEquals(3, result.getLanguageId());
    }

    @Test
    void getByVendorPlayerUsername_returnsLatestByCreateTime() {
        GameSession newer = session("GAME_A", 300L);
        GameSession older = session("GAME_A", 100L);
        when(repository.findByVendorPlayerUsername("player1")).thenReturn(List.of(older, newer));

        GameSession result = dataService.getByVendorPlayerUsername("player1", contextForGame("GAME_A"));

        assertSame(newer, result);
    }

    @Test
    void getByVendorPlayerUsername_throwsWhenNoSession() {
        when(repository.findByVendorPlayerUsername("player1")).thenReturn(List.of());

        assertThrows(GameSessionExpiredException.class,
                () -> dataService.getByVendorPlayerUsername("player1", contextForGame("GAME_A")));
    }
}
