package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.InvalidPlayerException;
import com.nextgen.gameaggregator.operator.game.url.GameUrlDto;
import com.nextgen.gameaggregator.repository.AgentPlayerRepository;
import com.nextgen.gameaggregator.repository.AgentRepository;
import com.nextgen.gameaggregator.repository.GameSessionRepository;
import com.nextgen.gameaggregator.repository.VendorPlayerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class GameSessionService {
    @Autowired
    private GameSessionRepository gameSessionRepository;
    @Autowired
    private VendorPlayerRepository vendorPlayerRepository;
    @Autowired
    private AgentPlayerRepository agentPlayerRepository;
    @Autowired
    private AgentRepository agentRepository;

    @Cacheable(value = "GameSessions", key = "#token")
    public GameSession verifyToken(String token) throws AuthenticationException {
        GameSession session = gameSessionRepository.findByToken(token);
        Optional.ofNullable(session).orElseThrow(AuthenticationException::new);

        //TODO (by Alex), validate gameId param from vendor is match with game_sessions table's vendor_game_id
        return session;
    }

    @Cacheable(value = "GameSessions", key = "#gameSession.token")
    public GameSession createSession(GameSession gameSession, GameUrlDto dto, VendorGame vendorGame, Currency currency) {
        gameSession.setTraceId(dto.getTraceId());
        gameSession.setLanguage(dto.getLanguage());
        gameSession.setVendorId(vendorGame.getVendorId());
        gameSession.setVendorGameId(vendorGame.getId());
        System.err.println(vendorGame.getVendorGameCode());
        gameSession.setVendorGameCode(vendorGame.getVendorGameCode());
        gameSession.setGameCategoryId(vendorGame.getGameCategoryId());
        gameSession.setCurrencyId(currency.getId());
        gameSession.setCurrencyCode(currency.getCode());

        gameSessionRepository.save(gameSession);

        return gameSession;
    }

    public String getPlayerCurrencyCode(Long agentPlayerId) throws InvalidPlayerException {
        // TODO: require optimisation
        Optional<AgentPlayer> agentPlayer = agentPlayerRepository.findById(agentPlayerId);
        agentPlayer.orElseThrow(InvalidPlayerException::new);

        Optional<Agent> agent = agentRepository.findById(agentPlayer.get().getAgentId());
        agent.orElseThrow(InvalidPlayerException::new);

        return agent.get().getCurrency().getCode();
    }
}
