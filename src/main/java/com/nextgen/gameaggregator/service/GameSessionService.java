package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.operator.game.url.GameUrlDto;
import com.nextgen.gameaggregator.repository.AgentPlayerRepository;
import com.nextgen.gameaggregator.repository.AgentRepository;
import com.nextgen.gameaggregator.repository.RawGameSessionRepository;
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
    private RawGameSessionRepository rawGameSessionRepository;

    @Autowired
    private AgentPlayerRepository agentPlayerRepository;
    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private RedisConnectionFactory connectionFactory;

    @Cacheable(value = "GameSessions", key = "#token", cacheManager = "cacheManager")
    public RawGameSession verifyToken(String token) throws AuthenticationException {
        RawGameSession session = rawGameSessionRepository.findByToken(token);
        Optional.ofNullable(session).orElseThrow(AuthenticationException::new);

        //TODO (by Alex), validate gameId param from vendor is match with game_sessions table's vendor_game_id
        return session;
    }

    //TODO, Figure a way to handle while connection lost to redis server, For Insert and Read
    @CachePut(value = "GameSessions", key = "#rawGameSession.token", cacheManager = "cacheManager")
    public RawGameSession createSession(RawGameSession rawGameSession, GameUrlDto dto, VendorGame vendorGame, VendorGameCode vendorGameCode,
                                        Currency currency, VendorCurrency vendorCurrency, VendorLanguageCode vendorLanguageCode,
                                        String vendorPlatformCode) {

        rawGameSession.setTraceId(dto.getTraceId());
        rawGameSession.setLanguage(dto.getLanguage());
        rawGameSession.setVendorId(vendorGame.getVendor().getId());
        rawGameSession.setVendorGameId(vendorGame.getId());
        rawGameSession.setVendorGameCode(vendorGameCode.getOpenGameCode());
        rawGameSession.setGameCategoryId(vendorGame.getGameCategory().getId());
        rawGameSession.setCurrencyId(currency.getId());
        rawGameSession.setCurrencyCode(currency.getCode());
        rawGameSession.setGameCode(vendorGame.getCode());
        rawGameSession.setVendorCurrencyCode(vendorCurrency.getVendorCurrencyCode());
        rawGameSession.setVendorLanguageCode(vendorLanguageCode.getLanguageCode());
        rawGameSession.setLanguageId(vendorGameCode.getLanguageId());
        rawGameSession.setPlatformId(vendorGameCode.getPlatformId());
        rawGameSession.setVendorPlatformCode(vendorPlatformCode);

        rawGameSessionRepository.save(rawGameSession);

        return rawGameSession;

    }

    @CachePut(value = "GameSessions", key = "#rawGameSession.vendorPlayerUsername", cacheManager = "cacheManager")
    public RawGameSession createSessionByVendorPlayer(RawGameSession rawGameSession){
        return rawGameSession;
    }

    @Cacheable(value = "GameSessions", key = "#username", cacheManager = "cacheManager")
    public RawGameSession getGameSessionByVendorPlayerUsername(String username) throws AuthenticationException {
        RawGameSession session = rawGameSessionRepository.findTop1ByVendorPlayerUsernameOrderByCreateTimeDesc(username);
        Optional.ofNullable(session).orElseThrow(AuthenticationException::new);

        return session;
    }

    @Cacheable(value = "GameSessions", key = "{#username, #vendorGameCode}", cacheManager = "cacheManager")
    public RawGameSession getGameSessionByVendorPlayerUsernameAndVendorGameCode(String username, String vendorGameCode) throws AuthenticationException {
        RawGameSession session = rawGameSessionRepository.findTop1ByVendorPlayerUsernameAndVendorGameCodeOrderByCreateTimeDesc(username, vendorGameCode);
        Optional.ofNullable(session).orElseThrow(AuthenticationException::new);

        return session;
    }
}
