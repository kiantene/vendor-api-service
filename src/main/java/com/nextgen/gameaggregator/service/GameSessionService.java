package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.*;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.game.url.GameLaunchDto;
import com.nextgen.gameaggregator.operator.game.url.GameUrlDto;
import com.nextgen.gameaggregator.repository.ga.writer.RawGameSessionRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@ComponentScan(basePackages = "com.nextgen.gameaggregator.redis.config")
public class GameSessionService {

    private final RawGameSessionRepository rawGameSessionRepository;
    private final CacheManager cacheManager;
    private final VendorPlayerService vendorPlayerService;
    private final AgentPlayerService agentPlayerService;
    private final VendorGameService vendorGameService;
    private final VendorCurrencyService vendorCurrencyService;
    private final CurrencyService currencyService;

    @Autowired
    public GameSessionService(RawGameSessionRepository rawGameSessionRepository,
                              CacheManager cacheManager,
                              VendorPlayerService vendorPlayerService,
                              AgentPlayerService agentPlayerService,
                              VendorGameService vendorGameService,
                              VendorCurrencyService vendorCurrencyService,
                              CurrencyService currencyService) {

        this.rawGameSessionRepository = rawGameSessionRepository;
        this.cacheManager = cacheManager;
        this.vendorPlayerService = vendorPlayerService;
        this.agentPlayerService = agentPlayerService;
        this.vendorGameService = vendorGameService;
        this.vendorCurrencyService = vendorCurrencyService;
        this.currencyService = currencyService;
    }

    @Cacheable(value = "GameSessions", key = "#token", cacheManager = "cacheManager")
    public GameSession verifyToken(String token) throws AuthenticationException {

        GameSession session = rawGameSessionRepository.findByToken(token);
        Optional.ofNullable(session).orElseThrow(AuthenticationException::new);

        return session;
    }

    @Cacheable(value = "GameSessions", key = "#vendorToken", cacheManager = "cacheManager")
    public GameSession verifyVendorToken(String vendorToken) throws AuthenticationException {

        GameSession session = rawGameSessionRepository.findByVendorToken(vendorToken);
        Optional.ofNullable(session).orElseThrow(AuthenticationException::new);

        return session;
    }

    @Caching(put = {
            @CachePut(value = "GameSessions", key = "{#gameSession.agentId, #gameSession.agentPlayerUsername, #gameSession.vendorLineId, #gameSession.currencyId}", cacheManager = "cacheManager"),
            @CachePut(value = "GameSessions", key = "#gameSession.token", cacheManager = "cacheManager"),
            @CachePut(value = "GameSessions", key = "#gameSession.vendorToken", cacheManager = "cacheManager"),
            @CachePut(value = "GameSessions", key = "#gameSession.vendorPlayerUsername", cacheManager = "cacheManager"),
            @CachePut(value = "GameSessions", key = "{#gameSession.vendorPlayerUsername, #gameSession.vendorGameCode}", cacheManager = "cacheManager"),
            @CachePut(value = "GameSessions", key = "#gameSession.vendorPlayerId", cacheManager = "cacheManager"),
            @CachePut(value = "GameSessions", key = "{#gameSession.vendorPlayerId, #gameSession.vendorGameCode}", cacheManager = "cacheManager"),
    })
    public GameSession updateSession(GameSession gameSession) {
        rawGameSessionRepository.save(gameSession);
        return gameSession;

    }

    @Caching(put = {
            @CachePut(value = "GameSessions", key = "#token", cacheManager = "cacheManager"),
            @CachePut(value = "GameSessions", key = "{#gameLaunchDto.agentId, #gameLaunchDto.agentPlayerUsername, #gameLaunchDto.vendorLineId, #gameLaunchDto.currencyId}", cacheManager = "cacheManager"),
            @CachePut(value = "GameSessions", key = "#gameLaunchDto.vendorPlayerUsername", cacheManager = "cacheManager"),
            @CachePut(value = "GameSessions", key = "{#gameLaunchDto.vendorPlayerUsername, #gameLaunchDto.openGameCode}", cacheManager = "cacheManager"),
            @CachePut(value = "GameSessions", key = "#gameLaunchDto.vendorPlayerId", cacheManager = "cacheManager"),
            @CachePut(value = "GameSessions", key = "{#gameLaunchDto.vendorPlayerId, #gameLaunchDto.openGameCode}", cacheManager = "cacheManager"),
    })
    public GameSession create(String token, GameUrlDto gameUrlDto, GameLaunchDto gameLaunchDto, VendorCurrency vendorCurrency, VendorLanguageCode vendorLanguageCode, String vendorPlatformCode) {

        GameSession entity = new GameSession();
        entity.setToken(token); // used for operator
        entity.setVendorToken(token); // used for vendor
        entity.setId(token);
        entity.setAgentId(gameLaunchDto.getAgentId());
        entity.setAgentPlayerId(gameLaunchDto.getAgentPlayerId());
        entity.setAgentPlayerUsername(gameLaunchDto.getAgentPlayerUsername());
        entity.setVendorPlayerId(gameLaunchDto.getVendorPlayerId());
        entity.setVendorPlayerUsername(gameLaunchDto.getVendorPlayerUsername());
        entity.setVendorLineId(gameLaunchDto.getVendorLineId());
        entity.setStatus(Status.ACTIVE.code);
        entity.setCreateTime(System.currentTimeMillis());
        entity.setTerminateTime(null);

        entity.setProductCode(gameLaunchDto.getProductCode());
        entity.setProductId(gameLaunchDto.getProductId());
        entity.setProductGameId(gameLaunchDto.getProductGameId());
        entity.setIsLaunchByProductGame(gameLaunchDto.getIsLaunchByProductGame());

        entity.setTraceId(gameUrlDto.getTraceId());
        entity.setLanguage(gameUrlDto.getLanguage());
        entity.setVendorId(gameLaunchDto.getVendorId());
        entity.setVendorGameId(gameLaunchDto.getVendorGameId());
        entity.setVendorGameCode(gameLaunchDto.getOpenGameCode());
        entity.setGameCategoryId(gameLaunchDto.getGameCategoryId());
        entity.setCurrencyId(gameLaunchDto.getCurrencyId());
        entity.setCurrencyCode(gameLaunchDto.getCurrencyCode());
        entity.setGameCode(gameUrlDto.getGameCode());
        entity.setVendorCurrencyCode(vendorCurrency.getVendorCurrencyCode());
        entity.setVendorLanguageCode(vendorLanguageCode.getLanguageCode());
        entity.setLanguageId(gameLaunchDto.getLanguageId());
        entity.setPlatformId(gameLaunchDto.getPlatformId());
        entity.setVendorPlatformCode(vendorPlatformCode);
        entity.setLobbyUrl(Optional.ofNullable(gameUrlDto.getLobbyUrl()).orElse(""));
        entity.setIpAddress(Optional.ofNullable(gameUrlDto.getIpAddress()).orElse(""));

        rawGameSessionRepository.save(entity);
        return entity;
    }

    //TODO, Figure a way to handle while connection lost to redis server, For Insert and Read
    @Caching(put = {
            @CachePut(value = "GameSessions", key = "{#gameSession.agentId, #gameSession.agentPlayerUsername, #gameSession.vendorLineId, #gameSession.currencyId}", cacheManager = "cacheManager"),
            @CachePut(value = "GameSessions", key = "#gameSession.token", cacheManager = "cacheManager"),
            @CachePut(value = "GameSessions", key = "#gameSession.vendorPlayerUsername", cacheManager = "cacheManager"),
            @CachePut(value = "GameSessions", key = "{#gameSession.vendorPlayerUsername, #vendorGameCode.openGameCode}", cacheManager = "cacheManager"),
            @CachePut(value = "GameSessions", key = "#gameSession.vendorPlayerId", cacheManager = "cacheManager"),
            @CachePut(value = "GameSessions", key = "{#gameSession.vendorPlayerId, #vendorGameCode.openGameCode}", cacheManager = "cacheManager"),
    })
    public GameSession createSession(GameSession gameSession, GameUrlDto dto, VendorGame vendorGame, VendorGameCode vendorGameCode,
                                     Integer currencyId, String currencyCode, VendorCurrency vendorCurrency, VendorLanguageCode vendorLanguageCode,
                                     String vendorPlatformCode, String lobbyUrl, String ipAddress) throws AuthenticationException {

        gameSession.setTraceId(dto.getTraceId());
        gameSession.setLanguage(dto.getLanguage());
        gameSession.setVendorId(vendorGame.getVendorId());
        gameSession.setVendorGameId(vendorGame.getId());
        gameSession.setVendorGameCode(vendorGameCode.getOpenGameCode());
        gameSession.setGameCategoryId(vendorGame.getGameCategoryId());
        gameSession.setCurrencyId(currencyId);
        gameSession.setCurrencyCode(currencyCode);
        gameSession.setGameCode(vendorGame.getCode());
        gameSession.setVendorCurrencyCode(vendorCurrency.getVendorCurrencyCode());
        gameSession.setVendorLanguageCode(vendorLanguageCode.getLanguageCode());
        gameSession.setLanguageId(vendorGameCode.getLanguageId());
        gameSession.setPlatformId(vendorGameCode.getPlatformId());
        gameSession.setVendorPlatformCode(vendorPlatformCode);
        gameSession.setLobbyUrl(Optional.ofNullable(lobbyUrl).orElse(""));
        gameSession.setIpAddress(Optional.ofNullable(ipAddress).orElse(""));

        rawGameSessionRepository.save(gameSession);
        return gameSession;

    }

    // deprecated, use getLastGameSessionByVendorPlayerUsername instead
    @CachePut(value = "GameSessions", key = "#username", cacheManager = "cacheManager")
    public GameSession getGameSessionByVendorPlayerUsername(String username) throws AuthenticationException {

        GameSession session = rawGameSessionRepository.findTop1ByVendorPlayerUsernameOrderByCreateTimeDesc(username);
        // use rawGameSessionRepository.findByVendorPlayerUsername()

        Optional.ofNullable(session).orElseThrow(AuthenticationException::new);

        return session;
    }

    @Cacheable(value = "GameSessions", key = "{#username, #vendorGameCode}", cacheManager = "cacheManager")
    public GameSession getByVendorPlayerUsername(String username, String vendorGameCode) {

        List<GameSession> gameSessionList;

        if (vendorGameCode == null) {
            gameSessionList = rawGameSessionRepository.findByVendorPlayerUsername(username);
        } else {
            gameSessionList = rawGameSessionRepository.findByVendorPlayerUsernameAndVendorGameCode(username, vendorGameCode);
        }

        if (gameSessionList.isEmpty()) {
            return null;
        } else {
            return gameSessionList.stream()
                    .max(Comparator.comparingLong(GameSession::getCreateTime))
                    .get();
        }

    }

    @Cacheable(value = "GameSessions", key = "#username", cacheManager = "cacheManager")
    public GameSession getLastGameSessionByVendorPlayerUsername(String username) {

        List<GameSession> gameSessionList = rawGameSessionRepository.findByVendorPlayerUsername(username);

        if (gameSessionList.isEmpty()) {
            return null;
        }

        return gameSessionList.stream()
                .max(Comparator.comparingLong(GameSession::getCreateTime))
                .get();
    }

    @Cacheable(value = "GameSessions", key = "{#username, #vendorGameCode}", cacheManager = "cacheManager")
    public GameSession getGameSessionByVendorPlayerUsernameAndVendorGameCode(String username, String vendorGameCode) throws AuthenticationException {
        GameSession session = rawGameSessionRepository.findTop1ByVendorPlayerUsernameAndVendorGameCodeOrderByCreateTimeDesc(username, vendorGameCode);
        Optional.ofNullable(session).orElseThrow(AuthenticationException::new);
        return session;
    }

    public void clearGameSession(GameSession gameSession, String username, String vendorGameCode) {
        cacheManager.getCache("GameSessions").evict(gameSession.getAgentId() + "," + username + "," + gameSession.getVendorLineId() + "," + gameSession.getCurrencyId());
        cacheManager.getCache("GameSessions").evict(gameSession.getToken());
        cacheManager.getCache("GameSessions").evict(gameSession.getVendorToken());
        cacheManager.getCache("GameSessions").evict(gameSession.getVendorPlayerUsername());
        cacheManager.getCache("GameSessions").evict(username);
        cacheManager.getCache("GameSessions").evict(gameSession.getVendorPlayerUsername() + "," + gameSession.getVendorGameCode());
        cacheManager.getCache("GameSessions").evict(gameSession.getVendorPlayerId());
        cacheManager.getCache("GameSessions").evict(gameSession.getVendorPlayerId() + "," + gameSession.getVendorGameCode());
        gameSession.setStatus(0);
        gameSession.setTerminateTime(System.currentTimeMillis());
        rawGameSessionRepository.save(gameSession);

    }


    public void terminateSessionByUserName(String userName) {
        List<GameSession> gameSessionList = rawGameSessionRepository.findByAgentPlayerUsernameAndStatus(userName, Status.ACTIVE.code);
        for (GameSession gameSession : gameSessionList) {
            this.clearGameSession(gameSession, gameSession.getAgentPlayerUsername(), gameSession.getVendorGameCode());
        }

    }

    @Caching(put = {
            @CachePut(value = "GameSessions", key = "#newToken", cacheManager = "cacheManager"),
            @CachePut(value = "GameSessions", key = "#gameSession.vendorToken", cacheManager = "cacheManager"),
            @CachePut(value = "GameSessions", key = "#gameSession.vendorPlayerUsername", cacheManager = "cacheManager"),
            @CachePut(value = "GameSessions", key = "{#gameSession.vendorPlayerUsername, #gameSession.vendorGameCode}", cacheManager = "cacheManager"),
            @CachePut(value = "GameSessions", key = "#gameSession.vendorPlayerId", cacheManager = "cacheManager"),
            @CachePut(value = "GameSessions", key = "{#gameSession.vendorPlayerId, #gameSession.vendorGameCode}", cacheManager = "cacheManager"),
            @CachePut(value = "GameSessions", key = "{#gameSession.agentId, #gameSession.agentPlayerUsername, #gameSession.vendorLineId, #gameSession.currencyId}", cacheManager = "cacheManager"),
    })
    public GameSession regenerateGameSessionToken(GameSession gameSession, String newToken) {

        // clear old GameSession in redis and couchbase
        clearGameSession(gameSession, gameSession.getAgentPlayerUsername(), gameSession.getVendorGameCode());

        // Mapping old GameSession into new GameSession
        GameSession newGameSession = new ModelMapper().map(gameSession, GameSession.class);

        newGameSession.setId(newToken);
        newGameSession.setToken(newToken);
        newGameSession.setStatus(Status.ACTIVE.code);
        newGameSession.setCreateTime(System.currentTimeMillis());
        newGameSession.setTerminateTime(null);

        rawGameSessionRepository.save(newGameSession);
        return newGameSession;
    }

    @Caching(put = {
            @CachePut(value = "GameSessions", key = "#newToken", cacheManager = "cacheManager"),
            @CachePut(value = "GameSessions", key = "#gameSession.token", cacheManager = "cacheManager"),
            @CachePut(value = "GameSessions", key = "#gameSession.vendorPlayerUsername", cacheManager = "cacheManager"),
            @CachePut(value = "GameSessions", key = "{#gameSession.vendorPlayerUsername, #gameSession.vendorGameCode}", cacheManager = "cacheManager"),
            @CachePut(value = "GameSessions", key = "#gameSession.vendorPlayerId", cacheManager = "cacheManager"),
            @CachePut(value = "GameSessions", key = "{#gameSession.vendorPlayerId, #gameSession.vendorGameCode}", cacheManager = "cacheManager"),
            @CachePut(value = "GameSessions", key = "{#gameSession.agentId, #gameSession.agentPlayerUsername, #gameSession.vendorLineId, #gameSession.currencyId}", cacheManager = "cacheManager"),
    })
    public GameSession regenerateVendorToken(GameSession gameSession, String newToken) {

        gameSession.setVendorToken(newToken);
        rawGameSessionRepository.save(gameSession);

        return gameSession;
    }

    @Transactional
    public VendorPlayer updateNewVendorPlayerUsername(GameSession gameSession, String newVendorPlayerUsername) throws InvalidPlayerException {

        VendorPlayer vendorPlayer = vendorPlayerService.getVendorPlayerByUsername(gameSession.getVendorPlayerUsername());
        vendorPlayer.setUsername(newVendorPlayerUsername);

        this.clearGameSession(gameSession, gameSession.getAgentPlayerUsername(), gameSession.getVendorGameCode());
        gameSession.setVendorPlayerUsername(newVendorPlayerUsername);
        gameSession.setStatus(Status.ACTIVE.code);
        this.updateSession(gameSession);

        return vendorPlayerService.saveAndFlush(vendorPlayer);
    }

    public GameSession generateNewSessionToken(String vendorPlayerUsername) throws InvalidPlayerException {
        VendorPlayer vendorPlayer = vendorPlayerService.getVendorPlayerByUsername(vendorPlayerUsername);
        Long agentPlayerId = vendorPlayer.getAgentPlayerId();

        AgentPlayer agentPlayer;

        try {
            agentPlayer = agentPlayerService.get(agentPlayerId);
        } catch (RecordNotFoundException recordNotFoundException) {
            throw new InvalidPlayerException("agentPlayerId " + agentPlayerId + " cannot be found");
        }

        GameSession gameSession = new GameSession();
        gameSession.setAgentId(agentPlayer.getAgentId());
        gameSession.setAgentPlayerId(agentPlayerId);
        gameSession.setAgentPlayerUsername(agentPlayer.getUsername());
        gameSession.setVendorId(vendorPlayer.getVendorId());
        gameSession.setVendorPlayerId(vendorPlayer.getId());
        gameSession.setVendorPlayerUsername(vendorPlayerUsername);
        gameSession.setVendorLineId(vendorPlayer.getVendorLineId());
        gameSession.setStatus(Status.ACTIVE.code);
        gameSession.setCurrencyId(vendorPlayer.getCurrencyId());

        return gameSession;
    }

    public GameSession generateNewSessionTokenByVendorPlayerId(Long vendorPlayerId) throws InvalidPlayerException {
        VendorPlayer vendorPlayer = vendorPlayerService.getByVendorPlayerId(vendorPlayerId, null);
        Long agentPlayerId = vendorPlayer.getAgentPlayerId();

        AgentPlayer agentPlayer;

        try {
            agentPlayer = agentPlayerService.get(agentPlayerId);
        } catch (RecordNotFoundException recordNotFoundException) {
            throw new InvalidPlayerException("agentPlayerId " + agentPlayerId + " cannot be found");
        }

        GameSession gameSession = new GameSession();
        gameSession.setAgentId(agentPlayer.getAgentId());
        gameSession.setAgentPlayerId(agentPlayerId);
        gameSession.setAgentPlayerUsername(agentPlayer.getUsername());
        gameSession.setVendorId(vendorPlayer.getVendorId());
        gameSession.setVendorPlayerId(vendorPlayer.getId());
        gameSession.setVendorPlayerUsername(vendorPlayer.getUsername());
        gameSession.setVendorLineId(vendorPlayer.getVendorLineId());
        gameSession.setStatus(Status.ACTIVE.code);
        gameSession.setCurrencyId(vendorPlayer.getCurrencyId());

        return gameSession;
    }

    public void updateByVendorGameCode(GameSession gameSession, String vendorGameCode) throws GameNotSupportedException {
        Integer vendorId = gameSession.getVendorId();

        if (vendorId == null) return;

        VendorGame vendorGame = vendorGameService.getByVendorGameCodeAndVendorId(vendorGameCode, vendorId);
        gameSession.setVendorGameId(vendorGame.getId());
        gameSession.setGameCode(vendorGame.getCode());
        gameSession.setVendorGameCode(vendorGameCode);
        gameSession.setGameCategoryId(vendorGame.getGameCategoryId());
    }

    public void updateByVendorCurrencyCode(GameSession gameSession, String vendorCurrencyCode) throws VendorCurrencyNotSupportException {
        Integer vendorId = gameSession.getVendorId();

        if (vendorId == null) return;

        VendorCurrency vendorCurrency = vendorCurrencyService.findByVendorIdAndVendorCurrencyCode(vendorId, vendorCurrencyCode);
        gameSession.setCurrencyId(vendorCurrency.getCurrencyId());
        gameSession.setVendorCurrencyCode(vendorCurrencyCode);

        Integer currencyId = vendorCurrency.getCurrencyId();
        String currencyCode = vendorCurrency.getVendorCurrencyCode();
        try {
            Currency currency = currencyService.get(currencyId);
            currencyCode = currency.getCode();
        } catch (InvalidCurrencyException invalidCurrencyException) {
            // do nothing to suppress the error
        }
        gameSession.setCurrencyCode(currencyCode);
    }

    public void updateByVendorCurrencyId(GameSession gameSession) throws VendorCurrencyNotSupportException {
        Integer vendorId = gameSession.getVendorId();
        Integer currencyId = gameSession.getCurrencyId();

        if (vendorId == null) return;

        VendorCurrency vendorCurrency = vendorCurrencyService.findByVendorIdAndCurrencyId(vendorId, currencyId);
        String currencyCode = vendorCurrency.getVendorCurrencyCode();

        gameSession.setCurrencyId(vendorCurrency.getCurrencyId());
        gameSession.setVendorCurrencyCode(vendorCurrency.getVendorCurrencyCode());

        try {
            Currency currency = currencyService.get(currencyId);
            currencyCode = currency.getCode();
        } catch (InvalidCurrencyException invalidCurrencyException) {
            // do nothing to suppress the error
        }
        gameSession.setCurrencyCode(currencyCode);
    }
}
