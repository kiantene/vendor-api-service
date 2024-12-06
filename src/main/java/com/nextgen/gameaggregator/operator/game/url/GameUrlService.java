package com.nextgen.gameaggregator.operator.game.url;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.ga.*;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.repository.ga.writer.*;
import com.nextgen.gameaggregator.service.AgentService;
import com.nextgen.gameaggregator.service.AgentServiceImpl;
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
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class GameUrlService {
    private static final String USERTYPE = "operator-api-service";
    private final AgentService agentService;
    private final AutowireCapableBeanFactory autowireCapableBeanFactory;
    private final RawGameSessionRepository rawGameSessionRepository;
    private final AgentPlayerRepository agentPlayerRepository;
    private final VendorPlayerRepository vendorPlayerRepository;
    private final VendorGameCodeRepository vendorGameCodeRepository;
    private final PlatformRepository platformRepository;
    private final VendorService vendorService;
    private final VendorGameCurrencyRepository vendorGameCurrencyRepository;
    private final CurrencyRepository currencyRepository;

    @Autowired
    public GameUrlService(AgentServiceImpl agentService,
                          AutowireCapableBeanFactory autowireCapableBeanFactory,
                          RawGameSessionRepository rawGameSessionRepository,
                          AgentPlayerRepository agentPlayerRepository,
                          VendorPlayerRepository vendorPlayerRepository,
                          VendorGameCodeRepository vendorGameCodeRepository,
                          PlatformRepository platformRepository,
                          VendorGameCurrencyRepository vendorGameCurrencyRepository,
                          CurrencyRepository currencyRepository,
                          VendorService vendorService) {

        this.agentService = agentService;
        this.autowireCapableBeanFactory = autowireCapableBeanFactory;
        this.rawGameSessionRepository = rawGameSessionRepository;
        this.agentPlayerRepository = agentPlayerRepository;
        this.vendorPlayerRepository = vendorPlayerRepository;
        this.vendorGameCodeRepository = vendorGameCodeRepository;
        this.platformRepository = platformRepository;
        this.vendorGameCurrencyRepository = vendorGameCurrencyRepository;
        this.currencyRepository = currencyRepository;
        this.vendorService = vendorService;
    }

    public GameUrlData getGameUrl(String gameCode, GameSession gameSession, Map<String, String> credentials,
                                  VendorLine vendorLine, HttpRequestLog httpRequestLog)
            throws InvalidVendorResponseException {

        GameUrlData gameUrlData = new GameUrlData();
        gameUrlData.setToken(gameSession.getToken());

        try {
            String vendorClassName = vendorService.getById(vendorLine.getVendorId()).getClassName();

            String className = "com.nextgen.gameaggregator.vendor." + vendorClassName + ".api.gameurl.GameUrlService";
            GameUrl gameUrl = (GameUrl) Class.forName(className).getConstructor().newInstance();
            autowireCapableBeanFactory.autowireBean(gameUrl);
            MultiValueMap<String, String> formData = gameUrl.formDataBuilder(gameCode, gameSession, credentials);

            httpRequestLog.setOperatorData(httpRequestLog.getRequestBody());
            httpRequestLog.setRequestBody(new Gson().toJson(formData.toSingleValueMap()));
            long startTime = System.currentTimeMillis();
            httpRequestLog.setBetStart(startTime);
            GameUrlVo gameUrlVo = gameUrl.callToVendor(formData, credentials, gameSession, httpRequestLog);

            if (gameUrlVo == null) throw new InvalidVendorResponseException();

            //token will be replaced if vendor's token is needed to verify for action files.
            gameUrlData.setToken(gameSession.getToken());
            gameUrlData.setGameUrl(gameUrlVo.getGameUrl());

            //TODO throw vendor maintenance exception

        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException | InvalidVendorLineException | InvalidVendorResponseException |
                 InvalidFormatException | InvalidVendorException | TimeoutException
                gameClassException) {

            throw new InvalidVendorResponseException(gameClassException.getMessage());

        }

        return gameUrlData;
    }

    @Cacheable(value = "Platforms", key = "#platformCode", cacheManager = "cacheManager")
    public Platform checkPlatformCode(String platformCode) throws InvalidPlatformException {
        Platform platform = platformRepository.findByCode(platformCode);
        Optional.ofNullable(platform).orElseThrow(InvalidPlatformException::new);
        return platform;

    }

    public VendorGameCode checkGameDetailSupported(GameLaunchDto gameLaunchDto)
            throws GameNotSupportedException, GameCurrencyNotSupportException {

        String openGameCode = gameLaunchDto.getOpenGameCode();
        Integer currencyId = gameLaunchDto.getCurrencyId();
        Integer platformId = gameLaunchDto.getPlatformId();
        Integer languageId = gameLaunchDto.getLanguageId();
        Integer vendorId = gameLaunchDto.getVendorId();
        Integer vendorGameId = gameLaunchDto.getVendorGameId();

        VendorGameCode vendorGameCode = vendorGameCodeRepository.findByOpenGameCodeAndPlatformIdAndLanguageIdAndStatusAndVendorId(openGameCode,
                platformId, languageId, Status.ACTIVE.code, vendorId);

        Optional.ofNullable(vendorGameCode).orElseThrow(GameNotSupportedException::new);

        VendorGameCurrency vendorGameCurrency = vendorGameCurrencyRepository.findByVendorGameIdAndCurrencyId(vendorGameId, currencyId);

        if (vendorGameCurrency == null || vendorGameCurrency.getStatus() == 0) {
            throw new GameCurrencyNotSupportException();
        }

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

    public void isCurrencySupportedByAgent(Integer agentId, Integer currencyId) throws CurrencyNotSupportedException {
        boolean isSupported = agentService.isCurrencySupportedByAgent(agentId, currencyId);

        if (!isSupported) {
            throw new CurrencyNotSupportedException("AgentId: " + agentId + " CurrencyId: " + currencyId);
        }
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

    @CachePut(value = "VendorPlayers", key = "{#agentPlayer.id, #vendorLine.id, #currencyId}", cacheManager = "cacheManager")
    public VendorPlayer checkVendorPlayer(AgentPlayer agentPlayer, VendorLine vendorLine, Integer currencyId) throws DisabledAgentPlayerException {
        VendorPlayer vendorPlayer = vendorPlayerRepository.findByAgentPlayerIdAndVendorLineIdAndCurrencyId(agentPlayer.getId(), vendorLine.getId(),
                currencyId);

        if (vendorPlayer == null) {
            vendorPlayer = this.createVendorPlayer(agentPlayer.getId(), vendorLine.getId(), vendorLine.getVendorId(), currencyId);
            vendorPlayerRepository.save(vendorPlayer);
        } else {
            if (vendorPlayer.getStatus().equals(Status.INACTIVE.code)) {
                throw new DisabledAgentPlayerException();
            }
        }
        return vendorPlayer;
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
