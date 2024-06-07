package com.nextgen.gameaggregator.operator.game.url;

import com.nextgen.gameaggregator.entity.ga.*;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.repository.ga.writer.*;
import com.nextgen.gameaggregator.service.VendorService;
import com.nextgen.gameaggregator.util.NameUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class GameUrlService {
    private static final String USERTYPE = "operator-api-service";
    private final AutowireCapableBeanFactory autowireCapableBeanFactory;
    private final RawGameSessionRepository rawGameSessionRepository;
    private final AgentPlayerRepository agentPlayerRepository;
    private final VendorPlayerRepository vendorPlayerRepository;
    private final VendorGameCodeRepository vendorGameCodeRepository;
    private final PlatformRepository platformRepository;
    private final VendorService vendorService;
    private final VendorGameCurrencyRepository vendorGameCurrencyRepository;
    private final CurrencyRepository currencyRepository;
    private final AgentCurrencyRepository agentCurrencyRepository;

    @Autowired
    public GameUrlService(AutowireCapableBeanFactory autowireCapableBeanFactory,
                          RawGameSessionRepository rawGameSessionRepository,
                          AgentPlayerRepository agentPlayerRepository,
                          VendorPlayerRepository vendorPlayerRepository,
                          VendorGameCodeRepository vendorGameCodeRepository,
                          PlatformRepository platformRepository,
                          VendorGameCurrencyRepository vendorGameCurrencyRepository,
                          CurrencyRepository currencyRepository,
                          AgentCurrencyRepository agentCurrencyRepository,
                          VendorService vendorService) {

        this.autowireCapableBeanFactory = autowireCapableBeanFactory;
        this.rawGameSessionRepository = rawGameSessionRepository;
        this.agentPlayerRepository = agentPlayerRepository;
        this.vendorPlayerRepository = vendorPlayerRepository;
        this.vendorGameCodeRepository = vendorGameCodeRepository;
        this.platformRepository = platformRepository;
        this.vendorGameCurrencyRepository = vendorGameCurrencyRepository;
        this.currencyRepository = currencyRepository;
        this.agentCurrencyRepository = agentCurrencyRepository;
        this.vendorService = vendorService;
    }

    public GameUrlData getGameUrl(VendorGame vendorGame, GameSession gameSession, Map<String, String> credentials,
                                  VendorLine vendorLine)
            throws InvalidVendorResponseException {

        GameUrlData gameUrlData = new GameUrlData();
        gameUrlData.setToken(gameSession.getToken());

        try {
            String vendorClassName = vendorService.getByVendorId(vendorLine.getVendorId(), null).getClassName();

            String className = "com.nextgen.gameaggregator.vendor." + vendorClassName + ".api.gameurl.GameUrlService";
            GameUrl gameUrl = (GameUrl) Class.forName(className).getConstructor().newInstance();
            autowireCapableBeanFactory.autowireBean(gameUrl);
            MultiValueMap<String, String> formData = gameUrl.formDataBuilder(vendorGame.getVendorGameCode(), gameSession, credentials);
            GameUrlVo gameUrlVo = gameUrl.call(formData, credentials, gameSession);

            Optional.ofNullable(gameUrlVo).orElseThrow(InvalidVendorResponseException::new);
            //token will be replaced if vendor's token is needed to verify for action files.
            gameUrlData.setToken(gameSession.getToken());
            gameUrlData.setGameUrl(gameUrlVo.getGameUrl());

            //TODO throw vendor maintenance exception

        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException |
                 InstantiationException | IllegalAccessException | InvalidVendorLineException |
                 InvalidVendorResponseException | InvalidFormatException | InvalidVendorException
                gameClassException) {
            //gameClassException.printStackTrace();

            log.error("GAME CLASS ERROR :");
            gameClassException.printStackTrace();
            throw new InvalidVendorResponseException();
        }

        return gameUrlData;
    }

    @Cacheable(value = "Platforms", key = "#platformCode", cacheManager = "cacheManager")
    public Platform checkPlatformCode(String platformCode) throws InvalidPlatformException {
        Platform platform = platformRepository.findByCode(platformCode);
        Optional.ofNullable(platform).orElseThrow(InvalidPlatformException::new);
        return platform;

    }

    public VendorGameCode checkGameDetailSupported(VendorGame vendorGame, Language language, Platform platform,
                                                   Currency currency)
            throws GameNotSupportedException, GameLanguageNotSupportException, GamePlatformNotSupportException, GameCurrencyNotSupportException {

        VendorGameCode vendorGameCode = vendorGameCodeRepository.findByOpenGameCodeAndPlatformIdAndLanguageIdAndStatusAndVendorId(vendorGame.getVendorGameCode(),
                platform.getId(), language.getId(), Status.ACTIVE.code, vendorGame.getVendorId());

        Optional.ofNullable(vendorGameCode).orElseThrow(GameNotSupportedException::new);

        VendorGameCurrency vendorGameCurrency = vendorGameCurrencyRepository.findByVendorGameIdAndCurrencyId(vendorGame.getId(), currency.getId());

        //not currency match with the requested game id
        Optional.ofNullable(vendorGameCurrency).orElseThrow(GameCurrencyNotSupportException::new);

        if (vendorGameCurrency.getStatus() == 0) {
            throw new GameCurrencyNotSupportException();
        }

        return vendorGameCode;
    }

    public VendorGameCode getFirstVendorGameCode(VendorGame vendorGame) throws GameNotSupportedException {

        VendorGameCode vendorGameCode = vendorGameCodeRepository.findTop1ByVendorGameIdAndStatus(vendorGame.getId(), Status.ACTIVE.code);
        //not vendor game id and language matched
        Optional.ofNullable(vendorGameCode).orElseThrow(GameNotSupportedException::new);

        return vendorGameCode;

    }

    public String getVendorPlatformCode(String className, Integer platformId) throws VendorPlatformNotSupportedException {

        //default value
        String vendorPlatformCode = (platformId == 1) ? "H5" : "WEB";

        try {
            String classNamePath = "com.nextgen.gameaggregator.vendor." + className + ".constant.Platforms";
            Class<?> c = Class.forName(classNamePath);
            Field field = c.getField(vendorPlatformCode);
            Object value = field.get(null);
            vendorPlatformCode = value.toString();
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            throw new VendorPlatformNotSupportedException();
        }

        return vendorPlatformCode;
    }

    public Currency checkCurrency(String currencyCode) throws InvalidCurrencyException {
        Currency currency = currencyRepository.findByCode(currencyCode);
        Optional.ofNullable(currency).orElseThrow(InvalidCurrencyException::new);
        return currency;
    }

    public AgentCurrency checkAgentCurrencySupported(Agent agent, Currency currency) throws CurrencyNotSupportedException {

        AgentCurrency agentCurrency = agentCurrencyRepository.
                findAgentCurrencyByAgentIdAndCurrencyIdAndStatus(agent.getId(), currency.getId(), Status.ACTIVE.code);
        Optional.ofNullable(agentCurrency).orElseThrow(CurrencyNotSupportedException::new);
        return agentCurrency;
    }

    public void checkDuplicateRequest(Integer agentId, String traceId) throws DuplicateRequestException {
        GameSession entity = rawGameSessionRepository.findByAgentIdAndTraceId(agentId, traceId);
        if (entity != null) {
            throw new DuplicateRequestException();
        }
    }

    @CachePut(value = "AgentPlayers", key = "{#agent.id, #username}", cacheManager = "cacheManager")
    public AgentPlayer checkAgentPlayer(Agent agent, String username) throws DisabledAgentPlayerException {
        AgentPlayer agentPlayer = agentPlayerRepository.findByAgentIdAndUsername(agent.getId(), username);
        if (agentPlayer == null) {
            agentPlayer = this.createAgentPlayer(agent.getId(), username);
            agentPlayerRepository.save(agentPlayer);
        } else {
            if (agentPlayer.getStatus().equals(Status.INACTIVE.code)) {
                throw new DisabledAgentPlayerException();
            }
        }
        return agentPlayer;
    }

    @CachePut(value = "VendorPlayers", key = "{#agentPlayer.id, #vendorLine.id, #currency.id}", cacheManager = "cacheManager")
    public VendorPlayer checkVendorPlayer(AgentPlayer agentPlayer, VendorLine vendorLine, Currency currency) throws DisabledAgentPlayerException {
        VendorPlayer vendorPlayer = vendorPlayerRepository.findByAgentPlayerIdAndVendorLineIdAndCurrencyId(agentPlayer.getId(), vendorLine.getId(),
                currency.getId());

        if (vendorPlayer == null) {
            vendorPlayer = this.createVendorPlayer(agentPlayer.getId(), vendorLine.getId(), vendorLine.getVendorId(), currency.getId());
            vendorPlayerRepository.save(vendorPlayer);
        } else {
            if (vendorPlayer.getStatus().equals(Status.INACTIVE.code)) {
                throw new DisabledAgentPlayerException();
            }
        }
        return vendorPlayer;
    }

    @CachePut(value = "GameSessions", key = "{#agent.id, #username, #vendorLine.id, #currency.id}", cacheManager = "cacheManager")
    public GameSession checkPlayer(Agent agent, String username, VendorLine vendorLine, Currency currency) throws DisabledAgentPlayerException {

        AgentPlayer agentPlayer = agentPlayerRepository.findByAgentIdAndUsername(agent.getId(), username);
        VendorPlayer vendorPlayer = null;
        Integer vendorId = vendorLine.getVendorId();

        if (agentPlayer == null) {
            agentPlayer = this.createAgentPlayer(agent.getId(), username);
            agentPlayerRepository.save(agentPlayer);
        } else {

            if (agentPlayer.getStatus().equals(Status.INACTIVE.code)) {
                throw new DisabledAgentPlayerException();
            }

            vendorPlayer = vendorPlayerRepository.findByAgentPlayerIdAndVendorLineIdAndCurrencyId(agentPlayer.getId(), vendorLine.getId(),
                    currency.getId());
        }

        if (vendorPlayer == null) {
            vendorPlayer = this.createVendorPlayer(agentPlayer.getId(), vendorLine.getId(), vendorId, currency.getId());
            vendorPlayerRepository.save(vendorPlayer);
        }

        return this.createGameSession(agentPlayer, vendorPlayer, vendorLine);
    }


    public AgentPlayer createAgentPlayer(Integer agentId, String username) {
        AgentPlayer entity = new AgentPlayer();
        entity.setAgentId(agentId);
        entity.setUsername(username);
        entity.setStatus(Status.ACTIVE.code);
        entity.prepareSave(0, USERTYPE);
        return entity;
    }

    public VendorPlayer createVendorPlayer(Long agentPlayerId, Integer vendorLineId, Integer vendorId, Integer currencyId) {

        String vendorPlayerUsername = NameUtils.generateUsername(vendorLineId.longValue(), agentPlayerId)
                + NameUtils.excelColumnNameFormula(currencyId);
        VendorPlayer entity = new VendorPlayer();
        entity.setAgentPlayerId(agentPlayerId);
        entity.setVendorLineId(vendorLineId);
        entity.setVendorId(vendorId);
        entity.setUsername(vendorPlayerUsername);
        entity.setStatus(Status.ACTIVE.code);
        entity.setCurrencyId(currencyId);
        entity.prepareSave(0, USERTYPE);
        return entity;
    }

    @CachePut(value = "GameSessions",
            key = "{#agentPlayer.agentId, #agentPlayer.username, #vendorLine.id, #vendorPlayer.currencyId}", cacheManager = "cacheManager")
    public GameSession createGameSession(AgentPlayer agentPlayer, VendorPlayer vendorPlayer, VendorLine vendorLine) {
        GameSession entity = new GameSession();

        entity.setToken(UUID.randomUUID().toString()); // used for operator
        entity.setVendorToken(entity.getToken()); // used for vendor
        entity.setId(entity.getToken());
        entity.setAgentId(agentPlayer.getAgentId());
        entity.setAgentPlayerId(agentPlayer.getId());
        entity.setAgentPlayerUsername(agentPlayer.getUsername());
        entity.setVendorPlayerUsername(vendorPlayer.getUsername());
        entity.setVendorPlayerId(vendorPlayer.getId());
        entity.setVendorLineId(vendorLine.getId());
        entity.setStatus(Status.ACTIVE.code);
        entity.setCreateTime(System.currentTimeMillis());
        entity.setTerminateTime(null);
        //entity.prepareSave(0, USERTYPE);

        return entity;
    }


}
