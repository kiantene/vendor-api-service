package com.nextgen.gameaggregator.operator.game.url;

import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.repository.*;
import com.nextgen.gameaggregator.util.NameUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

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

    private static final String USERTYPE = "operator-api-service";

    public GameUrlData getGameUrl(VendorGame vendorGame, GameSession gameSession, Map<String, String> credentials,
                                  VendorLine vendorLine)
            throws
            ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
            InstantiationException, IllegalAccessException, InvalidVendorLineException , InvalidVendorResponseException{

        String gameCode = vendorGame.getVendorGameCode();
        String token = gameSession.getToken();
        Integer vendorId = vendorGame.getVendorId();
        String className = "com.nextgen.gameaggregator.vendor."+vendorLine.getVendor().getClassName()+".api.gameurl.GameUrlService";

        GameUrl gameUrl = (GameUrl) Class.forName(className).getConstructor().newInstance();
        MultiValueMap<String, String> formData = gameUrl.formDataBuilder(gameCode, gameSession, credentials);
        GameUrlVo gameUrlVo = gameUrl.call(formData, credentials);

        GameUrlData gameUrlData = new GameUrlData();
        if (gameUrlVo != null) { // TODO: need to check error
            gameUrlData.setGameUrl(gameUrlVo.getGameUrl());
            gameUrlData.setToken(token);
        }

        return gameUrlData;
    }

    public VendorGame checkGameSupported(String gameCode) throws GameNotSupportedException {
        VendorGame entity = vendorGameRepository.findByCode(gameCode);
        Optional.ofNullable(entity).orElseThrow(GameNotSupportedException::new);

        if (entity.getStatus() == 0) {
            throw new GameNotSupportedException();
        }
        return entity;
    }

    public void checkCurrencySupported(Currency currency, String currencyCode) throws CurrencyNotSupportedException {
        if (!currency.getCode().equalsIgnoreCase(currencyCode)) {
            throw new CurrencyNotSupportedException();
        }
    }

    public void checkDuplicateRequest(Integer agentId, String traceId) throws DuplicateRequestException {
        GameSession entity = gameSessionRepository.findByAgentIdAndTraceId(agentId, traceId);
        if (entity != null) {
            throw new DuplicateRequestException();
        }
    }

    public GameSession checkPlayer(Integer agentId, String username, VendorLine vendorLine) {
        AgentPlayer agentPlayer = agentPlayerRepository.findByAgentIdAndUsername(agentId, username);
        VendorPlayer vendorPlayer = null;
        Integer vendorId = vendorLine.getVendor().getId();

        if (agentPlayer == null) {
            agentPlayer = this.createAgentPlayer(agentId, username);
            agentPlayerRepository.save(agentPlayer);
        } else {
            vendorPlayer = vendorPlayerRepository.findByAgentPlayerIdAndVendorLineId(agentPlayer.getId(), vendorLine.getId());
        }

        if (vendorPlayer == null) {
            vendorPlayer = this.createVendorPlayer(agentPlayer.getId(), vendorLine.getId(), vendorId);
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

    public VendorPlayer createVendorPlayer(Long agentPlayerId, Integer vendorLineId, Integer vendorId) {
        String vendorPlayerUsername = NameUtils.generateUsername("O", agentPlayerId, vendorLineId.longValue());
        VendorPlayer entity = new VendorPlayer();
        entity.setAgentPlayerId(agentPlayerId);
        entity.setVendorLineId(vendorLineId);
        entity.setVendorId(vendorId);
        entity.setUsername(vendorPlayerUsername);
        entity.setStatus(1);
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
