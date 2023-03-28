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
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
@ComponentScan(basePackages = "com.nextgen.gameaggregator.redis.config")
public class GameSessionService {
    @Autowired
    private GameSessionRepository gameSessionRepository;
    @Autowired
    private VendorPlayerRepository vendorPlayerRepository;
    @Autowired
    private AgentPlayerRepository agentPlayerRepository;
    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private RedisConnectionFactory connectionFactory;

    @Cacheable(value = "GameSessions", key = "#token", cacheManager = "cacheManager")
    public GameSession verifyToken(String token) throws AuthenticationException {
        GameSession session = gameSessionRepository.findByToken(token);
        Optional.ofNullable(session).orElseThrow(AuthenticationException::new);

        //TODO (by Alex), validate gameId param from vendor is match with game_sessions table's vendor_game_id
        return session;
    }

    //TODO, Figure a way to handle while connection lost to redis server, For Insert and Read
    @CachePut(value = "GameSessions", key = "#gameSession.token", cacheManager = "cacheManager")
    public GameSession createSession(GameSession gameSession, GameUrlDto dto, VendorGame vendorGame, VendorGameCode vendorGameCode,
                                     Currency currency, VendorLineCurrency vendorLineCurrency, String vendorLanguageCode,
                                     String vendorPlatformCode) {

        gameSession.setTraceId(dto.getTraceId());
        gameSession.setLanguage(dto.getLanguage());
        gameSession.setVendorId(vendorGame.getVendor().getId());
        gameSession.setVendorGameId(vendorGame.getId());
        gameSession.setVendorGameCode(vendorGameCode.getOpenGameCode());
        gameSession.setGameCategoryId(vendorGame.getGameCategory().getId());
        gameSession.setCurrencyId(currency.getId());
        gameSession.setCurrencyCode(currency.getCode());
        gameSession.setGameCode(vendorGame.getCode());
        gameSession.setVendorCurrencyCode(vendorLineCurrency.getVendorCurrencyCode());
        gameSession.setVendorLanguageCode(vendorLanguageCode);
        gameSession.setLanguageId(vendorGameCode.getLanguageId());
        gameSession.setPlatformId(vendorGameCode.getPlatformId());
        gameSession.setVendorPlatformCode(vendorPlatformCode);

        gameSessionRepository.save(gameSession);

        return gameSession;

    }

    @CachePut(value = "GameSessions", key = "#gameSession.vendorPlayerUsername", cacheManager = "cacheManager")
    public GameSession createSessionByVendorPlayer(GameSession gameSession){
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

    private boolean isRedisAvailable() {
        return connectionFactory.getConnection().ping() != null;
    }

    @Cacheable(value = "GameSessions", key = "#username", cacheManager = "cacheManager")
    public GameSession getGameSessionByVendorPlayerUsername(String username) throws AuthenticationException {
        GameSession session = gameSessionRepository.findTop1ByVendorPlayerUsernameOrderByIdDesc(username);
        Optional.ofNullable(session).orElseThrow(AuthenticationException::new);

        return session;
    }

    @Cacheable(value = "GameSessions", key = "{#username, #vendorGameCode}", cacheManager = "cacheManager")
    public GameSession getGameSessionByVendorPlayerUsernameAndVendorGameCode(String username, String vendorGameCode) throws AuthenticationException {
        GameSession session = gameSessionRepository.findTop1ByVendorPlayerUsernameAndVendorGameCodeOrderByIdDesc(username, vendorGameCode);
        Optional.ofNullable(session).orElseThrow(AuthenticationException::new);

        return session;
    }
}
