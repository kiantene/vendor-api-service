package com.nextgen.gameaggregator.operator.game.url;

import com.nextgen.gameaggregator.entity.ga.*;
import com.nextgen.gameaggregator.entity.ga.Currency;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.repository.ga.writer.*;
import com.nextgen.gameaggregator.service.AgentApiCredentialService;
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
import java.util.*;

@Service
@Slf4j
public class GameUrlService {

    @Autowired
    private AutowireCapableBeanFactory autowireCapableBeanFactory;
    @Autowired
    private RawGameSessionRepository rawGameSessionRepository;
    @Autowired
    private AgentPlayerRepository agentPlayerRepository;
    @Autowired
    private VendorPlayerRepository vendorPlayerRepository;
    @Autowired
    private VendorGameCodeRepository vendorGameCodeRepository;
    @Autowired
    private PlatformRepository platformRepository;
    @Autowired
    VendorGameCurrencyRepository vendorGameCurrencyRepository;
    @Autowired
    CurrencyRepository currencyRepository;
    @Autowired
    AgentCurrencyRepository agentCurrencyRepository;
    @Autowired
    AgentApiCredentialService agentApiCredentialService;

    private static final String USERTYPE = "operator-api-service";

    public GameUrlData getGameUrl(VendorGame vendorGame, GameSession gameSession, Map<String, String> credentials,
                                  VendorLine vendorLine)
            throws InvalidVendorResponseException {

        GameUrlData gameUrlData = new GameUrlData();
        gameUrlData.setToken(gameSession.getToken());
        try {
            String className = "com.nextgen.gameaggregator.vendor." + vendorLine.getVendor().getClassName() + ".api.gameurl.GameUrlService";
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
                 InvalidVendorResponseException | InvalidFormatException
                gameClassException) {
            //gameClassException.printStackTrace();

            log.error("GAME CLASS ERROR :");
            gameClassException.printStackTrace();
            throw new InvalidVendorResponseException();
        }

        return gameUrlData;
    }

    @Cacheable(value = "Platforms", key = "#platformCode" , cacheManager = "cacheManager")
    public Platform checkPlatformCode(String platformCode) throws InvalidPlatformException {
        Platform platform = platformRepository.findByCode(platformCode);
        Optional.ofNullable(platform).orElseThrow(InvalidPlatformException::new);
        return platform;

    }
    public VendorGameCode checkGameDetailSupported(VendorGame vendorGame, Language language, Platform platform, Currency currency)
            throws GameNotSupportedException, GameLanguageNotSupportException, GamePlatformNotSupportException, GameCurrencyNotSupportException {

        VendorGameCode vendorGameCode = vendorGameCodeRepository.findByOpenGameCodeAndPlatformIdAndLanguageIdAndStatusAndVendorId(vendorGame.getVendorGameCode(),
                platform.getId(), language.getId(), Status.ACTIVE.code, vendorGame.getVendor().getId());

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

    public Currency checkCurrency( String currencyCode) throws InvalidCurrencyException {
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

    @CachePut(value = "GameSessions", key = "{#agent.id, #username, #vendorLine.id, #currency.id}" , cacheManager = "cacheManager")
    public GameSession checkPlayer(Agent agent, String username, VendorLine vendorLine, Currency currency) throws DisabledAgentPlayerException {
        AgentPlayer agentPlayer = agentPlayerRepository.findByAgentIdAndUsername(agent.getId(), username);
        VendorPlayer vendorPlayer = null;
        Integer vendorId = vendorLine.getVendor().getId();

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
        String vendorPlayerUsername = NameUtils.generateUsername(vendorLineId.longValue(), Long.valueOf(currencyId), agentPlayerId )
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

    private GameSession createGameSession(AgentPlayer agentPlayer, VendorPlayer vendorPlayer, VendorLine vendorLine) {
        GameSession entity = new GameSession();

        entity.setToken(UUID.randomUUID().toString());
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
