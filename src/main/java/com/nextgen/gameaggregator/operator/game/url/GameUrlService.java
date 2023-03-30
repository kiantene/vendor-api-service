package com.nextgen.gameaggregator.operator.game.url;

import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.repository.*;
import com.nextgen.gameaggregator.util.NameUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private VendorGameRepository vendorGameRepository;
    @Autowired
    private GameSessionRepository gameSessionRepository;
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
    private VendorRepository vendorRepository;

    private static final String USERTYPE = "operator-api-service";

    public GameUrlData getGameUrl(VendorGame vendorGame, GameSession gameSession, Map<String, String> credentials,
                                  VendorLine vendorLine)
            throws
            ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
            InstantiationException, IllegalAccessException, InvalidVendorLineException, InvalidVendorResponseException, InvalidFormatException {

        String gameCode = vendorGame.getVendorGameCode();
        String token = gameSession.getToken();
        Integer vendorId = vendorGame.getVendor().getId();
        String className = "com.nextgen.gameaggregator.vendor."+vendorLine.getVendor().getClassName()+".api.gameurl.GameUrlService";

        GameUrl gameUrl = (GameUrl) Class.forName(className).getConstructor().newInstance();
        MultiValueMap<String, String> formData = gameUrl.formDataBuilder(gameCode, gameSession, credentials);
        GameUrlVo gameUrlVo = gameUrl.call(formData, credentials, gameSession);

        GameUrlData gameUrlData = new GameUrlData();
        if (gameUrlVo != null) { // TODO: need to check error
            gameUrlData.setGameUrl(gameUrlVo.getGameUrl());
            gameUrlData.setToken(token);
        }

        return gameUrlData;
    }

    public VendorGame checkGameSupported(String gameCode) throws GameNotSupportedException {
        VendorGame vendorGameEntity = vendorGameRepository.findByCode(gameCode);
        Optional.ofNullable(vendorGameEntity).orElseThrow(GameNotSupportedException::new);

        if (vendorGameEntity.getStatus() == 0) {
            throw new GameNotSupportedException();
        }

        return vendorGameEntity;
    }

    public VendorGameCode checkGameDetailSupported(Integer gameId, String platformCode, String languageCode) throws GameNotSupportedException {

        Platform platformEntity = platformRepository.findByCode(platformCode);
        Optional.ofNullable(platformEntity).orElseThrow(GameNotSupportedException::new);

        Languages languagesEntity = languageRepository.findByCode(languageCode);
        Optional.ofNullable(languagesEntity).orElseThrow(GameNotSupportedException::new);

        VendorGameCode vendorGameCodeEntity = vendorGameCodeRepository.findByVendorGameIdAndPlatformIdAndLanguageId(
                gameId, platformEntity.getId(), languagesEntity.getId());
        Optional.ofNullable(vendorGameCodeEntity).orElseThrow(GameNotSupportedException::new);

        if (vendorGameCodeEntity.getStatus() == 0) {
            throw new GameNotSupportedException();
        }

        return vendorGameCodeEntity;
    }

    public String getVendorPlatformCode(String className, Integer platformId){

        //default value
        String vendorPlatformCode = platformId == 1 ? "H5" : "WEB";

        try {
            String classNamePath = "com.nextgen.gameaggregator.vendor."+className+".constant.Platforms";
            Class<?> c = Class.forName(classNamePath);
            Field field = c.getField(vendorPlatformCode);
            Object value = field.get(null);
            vendorPlatformCode = value.toString();
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            //use default value if the constant or path is not exists
        }


        return vendorPlatformCode;
    }

    public void checkCurrencySupported(Currency currency, String currencyCode) throws CurrencyNotSupportedException {
        if (!currency.getCode().equalsIgnoreCase(currencyCode)) {
            throw new CurrencyNotSupportedException();
        }
    }

    public String checkVendorLanguageSupported(Integer vendorId, Integer languageId) throws VendorLanguageNotSupportedException {

        VendorLanguageCode vendorLanguageCodeEntity = vendorLanguageCodeRepository.findByVendorIdAndLanguageId(vendorId, languageId);
        Optional.ofNullable(vendorLanguageCodeEntity).orElseThrow(VendorLanguageNotSupportedException::new);

        if (vendorLanguageCodeEntity.getStatus() == 0) {
            throw new VendorLanguageNotSupportedException();
        }

        return vendorLanguageCodeEntity.getLanguageCode();
    }

    public void checkDuplicateRequest(Integer agentId, String traceId) throws DuplicateRequestException {
        GameSession entity = gameSessionRepository.findByAgentIdAndTraceId(agentId, traceId);
        if (entity != null) {
            throw new DuplicateRequestException();
        }
    }

    public GameSession checkPlayer(Integer agentId, String username, VendorLine vendorLine, Integer currencyId) {
        AgentPlayer agentPlayer = agentPlayerRepository.findByAgentIdAndUsername(agentId, username);
        VendorPlayer vendorPlayer = null;
        Integer vendorId = vendorLine.getVendor().getId();

        if (agentPlayer == null) {
            agentPlayer = this.createAgentPlayer(agentId, username);
            agentPlayerRepository.save(agentPlayer);
        } else {
            vendorPlayer = vendorPlayerRepository.findByAgentPlayerIdAndVendorLineIdAndCurrencyId(agentPlayer.getId(), vendorLine.getId(),
                    currencyId);
        }

        if (vendorPlayer == null) {
            vendorPlayer = this.createVendorPlayer(agentPlayer.getId(), vendorLine.getId(), vendorId, currencyId);
            vendorPlayerRepository.save(vendorPlayer);
        }

        log.info(agentPlayer.toString());
        log.info(vendorPlayer.toString());

        return this.createGameSession(agentPlayer, vendorPlayer, vendorLine);
    }

    public AgentPlayer createAgentPlayer(Integer agentId, String username) {
        AgentPlayer entity = new AgentPlayer();
        entity.setAgentId(agentId);
        entity.setUsername(username);
        entity.setStatus(1);
        entity.prepareSave(0, USERTYPE);
        log.info("Insert new agent player " + username);

        return entity;
    }

    public VendorPlayer createVendorPlayer(Long agentPlayerId, Integer vendorLineId, Integer vendorId, Integer currencyId) {
        String vendorPlayerUsername = NameUtils.generateUsername(vendorLineId.longValue(), agentPlayerId);
        VendorPlayer entity = new VendorPlayer();
        entity.setAgentPlayerId(agentPlayerId);
        entity.setVendorLineId(vendorLineId);
        entity.setVendorId(vendorId);
        entity.setUsername(vendorPlayerUsername);
        entity.setStatus(1); // TODO: to use constant/enum
        entity.setCurrencyId(currencyId);
        entity.prepareSave(0, USERTYPE);
        log.info("Insert new vendor player " + vendorPlayerUsername);

        return entity;
    }

    private GameSession createGameSession(AgentPlayer agentPlayer, VendorPlayer vendorPlayer, VendorLine vendorLine) {
        GameSession entity = new GameSession();

        entity.setToken(UUID.randomUUID().toString());
        entity.setAgentId(agentPlayer.getAgentId());
        entity.setAgentPlayerId(agentPlayer.getId());
        entity.setAgentPlayerUsername(agentPlayer.getUsername());
        entity.setVendorPlayerUsername(vendorPlayer.getUsername());
        entity.setVendorPlayerId(vendorPlayer.getId());
        entity.setVendorLineId(vendorLine.getId());
        entity.setStatus(1); // TODO: to use constant/enum
        entity.prepareSave(0, USERTYPE);

        return entity;
    }
}
