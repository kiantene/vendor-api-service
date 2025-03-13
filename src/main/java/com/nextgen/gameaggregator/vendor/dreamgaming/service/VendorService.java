package com.nextgen.gameaggregator.vendor.dreamgaming.service;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.InvalidPlayerException;
import com.nextgen.gameaggregator.exception.VendorCurrencyNotSupportException;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.vendor.dreamgaming.api.bet.BetDto;
import com.nextgen.gameaggregator.vendor.dreamgaming.api.rollback.RollbackDto;
import com.nextgen.gameaggregator.vendor.dreamgaming.constant.Credentials;
import com.nextgen.gameaggregator.vendor.dreamgaming.constant.Formats;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class VendorService extends BaseVendorService {
    private final GameSessionService gameSessionService;

    public VendorService(GameSessionService gameSessionService) {
        this.gameSessionService = gameSessionService;
    }

    public static String md5Generator(String input) {
        return DigestUtils.md5Hex(input);
    }

    public static String signGenerator(Map<String, String> credentials, String timeStamp) {
        return md5Generator(credentials.get(Credentials.AGENT_ID) + credentials.get(Credentials.API_KEY) + timeStamp);
    }

    public static String removeLeadingZero(String input) {
        return input.replaceAll(Formats.JSON_LEADING_ZERO, "$1");
    }

    public GameSession checkGameSession(String traceId, BetDto betDto) throws VendorCurrencyNotSupportException, InvalidPlayerException {
        GameSession gameSession;
        try {
            gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(betDto.getMember().getUsername().toLowerCase());
        } catch (AuthenticationException authenticationException) {
            gameSession = gameSessionService.generateNewSessionToken(betDto.getMember().getUsername().toLowerCase());
            gameSessionService.updateByVendorCurrencyId(gameSession);
            gameSession.setToken(traceId);
            gameSession.setVendorToken(traceId);
        }
        return gameSession;
    }

    public GameSession checkGameSession(String traceId, RollbackDto rollbackDto) throws VendorCurrencyNotSupportException, InvalidPlayerException {
        GameSession gameSession;
        try {
            gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(rollbackDto.getMember().getUsername().toLowerCase());
        } catch (AuthenticationException authenticationException) {
            gameSession = gameSessionService.generateNewSessionToken(rollbackDto.getMember().getUsername().toLowerCase());
            gameSessionService.updateByVendorCurrencyId(gameSession);
            gameSession.setToken(traceId);
            gameSession.setVendorToken(traceId);
        }
        return gameSession;
    }
}
