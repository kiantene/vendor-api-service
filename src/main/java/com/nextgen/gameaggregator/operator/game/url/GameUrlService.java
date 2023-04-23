package com.nextgen.gameaggregator.operator.game.url;

import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.repository.*;
import com.nextgen.gameaggregator.util.NameUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class GameUrlService {

    @Autowired
    private VendorGameRepository vendorGameRepository;
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
    private LanguageRepository languageRepository;
    @Autowired
    private VendorLanguageCodeRepository vendorLanguageCodeRepository;
    @Autowired
    VendorGameCurrencyRepository vendorGameCurrencyRepository;
    @Autowired
    private VendorRepository vendorRepository;

    private static final String USERTYPE = "operator-api-service";

    public GameUrlData getGameUrl(VendorGame vendorGame, GameSession gameSession, Map<String, String> credentials,
                                  VendorLine vendorLine)
            throws InvalidVendorResponseException {

        GameUrlData gameUrlData = new GameUrlData();
        gameUrlData.setToken(gameSession.getToken());
        try {
            String className = "com.nextgen.gameaggregator.vendor." + vendorLine.getVendor().getClassName() + ".api.gameurl.GameUrlService";
            GameUrl gameUrl = (GameUrl) Class.forName(className).getConstructor().newInstance();
            MultiValueMap<String, String> formData = gameUrl.formDataBuilder(vendorGame.getVendorGameCode(), gameSession, credentials);
            GameUrlVo gameUrlVo = gameUrl.call(formData, credentials, gameSession);

            Optional.ofNullable(gameUrlVo).orElseThrow(InvalidVendorResponseException::new);

            gameUrlData.setGameUrl(gameUrlVo.getGameUrl());

            //TODO throw vendor maintenance exception

        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException |
                 InstantiationException | IllegalAccessException | InvalidVendorLineException |
                 InvalidVendorResponseException | InvalidFormatException
                gameClassException) {
            gameClassException.printStackTrace();
            log.error("GAME CLASS ERROR :"+gameClassException.getStackTrace().toString());
            throw new InvalidVendorResponseException();
        }

        return gameUrlData;
    }

    public Platform checkPlatformCode(String platformCode) throws InvalidPlatformException {
        Platform platform = platformRepository.findByCode(platformCode);
        Optional.ofNullable(platform).orElseThrow(InvalidPlatformException::new);
        return platform;

    }



    public VendorGameCode checkGameDetailSupported(VendorGame vendorGame, Language language, Platform platform, Currency currency)
            throws GameNotSupportedException, GameLanguageNotSupportException, GamePlatformNotSupportException, GameCurrencyNotSupportException {


        List<VendorGameCode> vendorGameCodes = vendorGameCodeRepository.findByVendorGameIdAndLanguageIdAndStatus(vendorGame.getId(), language.getId(), Status.ACTIVE.code);
        //not vendor game id and language matched
        if (vendorGameCodes.isEmpty()) {
            throw new GameLanguageNotSupportException();
        }

        VendorGameCode vendorGameCodeMatched = null;
        //search the game supported platform
        for (VendorGameCode vendorGameCode : vendorGameCodes) {
            if (vendorGameCode.getPlatformId().equals(platform.getId())) {
                vendorGameCodeMatched = vendorGameCode;
                break;
            }
        }
        //not platform match with the requested game id
        Optional.ofNullable(vendorGameCodeMatched).orElseThrow(GamePlatformNotSupportException::new);

        VendorGameCurrency vendorGameCurrency = vendorGameCurrencyRepository.findByVendorGameIdAndCurrencyId(vendorGame.getId(), currency.getId());

        //not currency match with the requested game id
        Optional.ofNullable(vendorGameCurrency).orElseThrow(GameCurrencyNotSupportException::new);

        if (vendorGameCurrency.getStatus() == 0) {
            throw new GameCurrencyNotSupportException();
        }

        return vendorGameCodeMatched;
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

    public void checkAgentCurrencySupported(Currency currency, String currencyCode) throws CurrencyNotSupportedException {
        if (!currency.getCode().equalsIgnoreCase(currencyCode)) {
            throw new CurrencyNotSupportedException();
        }
    }

    public void checkDuplicateRequest(Integer agentId, String traceId) throws DuplicateRequestException {
        GameSession entity = rawGameSessionRepository.findByAgentIdAndTraceId(agentId, traceId);
        if (entity != null) {
            throw new DuplicateRequestException();
        }
    }

    public GameSession checkPlayer(Agent agent, String username, VendorLine vendorLine, Currency currency) throws DisabledAgentPlayerException {
        AgentPlayer agentPlayer = agentPlayerRepository.findByAgentIdAndUsername(agent.getId(), username);
        VendorPlayer vendorPlayer = null;
        Integer vendorId = vendorLine.getVendor().getId();

        if (agentPlayer == null) {
            agentPlayer = this.createAgentPlayer(agent.getId(), username);
            agentPlayerRepository.save(agentPlayer);
        } else {

            if(agentPlayer.getStatus().equals(Status.INACTIVE.code)){
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
        String vendorPlayerUsername = NameUtils.generateUsername(vendorLineId.longValue(), agentPlayerId);
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
