package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.operator.game.url.GameUrlDto;
import com.nextgen.gameaggregator.repository.AgentPlayerRepository;
import com.nextgen.gameaggregator.repository.AgentRepository;
import com.nextgen.gameaggregator.repository.RawGameSessionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@ComponentScan(basePackages = "com.nextgen.gameaggregator.redis.config")
public class GameSessionService {
    @Autowired
    private RawGameSessionRepository rawGameSessionRepository;

    @Autowired
    private AgentPlayerRepository agentPlayerRepository;
    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private RedisConnectionFactory connectionFactory;

    @Autowired
    private Environment environment;

    @Autowired
    private CacheManager cacheManager;

    @Cacheable(value = "GameSessions", key = "#token", cacheManager = "cacheManager")
    public GameSession verifyToken(String token) throws AuthenticationException {

        GameSession session = rawGameSessionRepository.findByToken(token);
        Optional.ofNullable(session).orElseThrow(AuthenticationException::new);

        //TODO (by Alex), validate gameId param from vendor is match with game_sessions table's vendor_game_id
        return session;
    }

    //TODO, Figure a way to handle while connection lost to redis server, For Insert and Read
    @Caching( put = {
            @CachePut(value = "GameSessions", key = "#gameSession.token" , cacheManager = "cacheManager"),
            @CachePut(value = "GameSessions", key = "#gameSession.vendorPlayerUsername", cacheManager = "cacheManager"),
            @CachePut(value = "GameSessions", key = "{#gameSession.vendorPlayerUsername, #vendorGameCode}", cacheManager = "cacheManager"),
    })
    public GameSession createSession(GameSession gameSession, GameUrlDto dto, VendorGame vendorGame, VendorGameCode vendorGameCode,
                                     Currency currency, VendorCurrency vendorCurrency, VendorLanguageCode vendorLanguageCode,
                                     String vendorPlatformCode) throws AuthenticationException {

        gameSession.setTraceId(dto.getTraceId());
        gameSession.setLanguage(dto.getLanguage());
        gameSession.setVendorId(vendorGame.getVendor().getId());
        gameSession.setVendorGameId(vendorGame.getId());
        gameSession.setVendorGameCode(vendorGameCode.getOpenGameCode());
        gameSession.setGameCategoryId(vendorGame.getGameCategory().getId());
        gameSession.setCurrencyId(currency.getId());
        gameSession.setCurrencyCode(currency.getCode());
        gameSession.setGameCode(vendorGame.getCode());
        gameSession.setVendorCurrencyCode(vendorCurrency.getVendorCurrencyCode());
        gameSession.setVendorLanguageCode(vendorLanguageCode.getLanguageCode());
        gameSession.setLanguageId(vendorGameCode.getLanguageId());
        gameSession.setPlatformId(vendorGameCode.getPlatformId());
        gameSession.setVendorPlatformCode(vendorPlatformCode);

        rawGameSessionRepository.save(gameSession);
        return gameSession;

    }

//    @CachePut(value = "GameSessions", key = "#gameSession.vendorPlayerUsername", cacheManager = "cacheManager")
//    public GameSession createSessionByVendorPlayer(GameSession gameSession){
//        return gameSession;
//    }

    @CachePut(value = "GameSessions", key = "#username", cacheManager = "cacheManager")
    public GameSession getGameSessionByVendorPlayerUsername(String username) throws AuthenticationException {
        GameSession session = rawGameSessionRepository.findTop1ByVendorPlayerUsernameOrderByCreateTimeDesc(username);
        Optional.ofNullable(session).orElseThrow(AuthenticationException::new);

        return session;
    }

    @Cacheable(value = "GameSessions", key = "{#username, #vendorGameCode}", cacheManager = "cacheManager")
    public GameSession getGameSessionByVendorPlayerUsernameAndVendorGameCode(String username, String vendorGameCode) throws AuthenticationException {
        GameSession session = rawGameSessionRepository.findTop1ByVendorPlayerUsernameAndVendorGameCodeOrderByCreateTimeDesc(username, vendorGameCode);
        Optional.ofNullable(session).orElseThrow(AuthenticationException::new);
        return session;
    }

    public void clearGameSession(GameSession gameSession, String username, String vendorGameCode){
        cacheManager.getCache("GameSessions").evict(gameSession.getToken());
        cacheManager.getCache("GameSessions").evict(gameSession.getVendorPlayerUsername());
        cacheManager.getCache("GameSessions").evict(username);
        cacheManager.getCache("GameSessions").evict(gameSession.getVendorPlayerUsername()+","+gameSession.getVendorGameCode());
        gameSession.setStatus(0);
        gameSession.setTerminateTime(System.currentTimeMillis());
        rawGameSessionRepository.save(gameSession);

    }


    public void terminateSessionByUserName(String userName) throws AuthenticationException {
         List<GameSession> gameSessionList = rawGameSessionRepository.findByAgentPlayerUsernameAndStatus(userName, Status.ACTIVE.code);
        for (GameSession gameSession : gameSessionList) {
            this.clearGameSession(gameSession, gameSession.getAgentPlayerUsername(), gameSession.getVendorGameCode());
        }

    }

}
