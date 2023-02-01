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
    public GameSession createSession(GameSession gameSession, GameUrlDto dto, VendorGame vendorGame, Currency currency, VendorLineCurrency vendorLineCurrency) {

        gameSession.setTraceId(dto.getTraceId());
        gameSession.setLanguage(dto.getLanguage());
        gameSession.setVendorId(vendorGame.getVendorId());
        gameSession.setVendorGameId(vendorGame.getId());
        //TODO (by Alex) to change to vendor game code table value
        gameSession.setVendorGameCode(vendorGame.getVendorGameCode());
        gameSession.setGameCategoryId(vendorGame.getGameCategoryId());
        gameSession.setCurrencyId(currency.getId());
        gameSession.setCurrencyCode(currency.getCode());
        gameSession.setGameCode(vendorGame.getCode());
        gameSession.setVendorCurrencyCode(vendorLineCurrency.getVendorCurrencyCode());
        //TODO (by Alex) temporary hard code
        //TODO value from vendor_language_codes table, language_code
        gameSession.setVendorLanguageCode("en");
        //TODO value from languages table, ID
        gameSession.setLanguageId(3);
        //TODO value from platforms table, ID
        gameSession.setPlatformId(1);
        //TODO value from vendor platform constant
        gameSession.setVendorPlatformCode("MOBILE");

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
}
