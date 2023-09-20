package com.nextgen.gameaggregator.vendor.playngo.service;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.VendorGameCode;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.CredentialNotFoundException;
import com.nextgen.gameaggregator.exception.GameNotSupportedException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.VendorGameCodeService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.playngo.constant.Credentials;
import com.nextgen.gameaggregator.vendor.playngo.dto.CommonDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;

@Service
@Slf4j
public class VendorService extends BaseVendorService {

    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private VendorGameCodeService vendorGameCodeService;

    public void verifyVendorGameCode(GameSession gameSession, String gameId) throws GameNotSupportedException {
        VendorGameCode vendorGameCode = vendorGameCodeService.getByVendorGameIdAndPlatformIdAndLanguageId(gameSession.getVendorGameId(), gameSession.getPlatformId(), gameSession.getLanguageId());
        if (!vendorGameCode.getBetGameCode().equals(gameId)) {
            throw new GameNotSupportedException();
        }
    }

    public void verifyAccessCode(Integer vendorLineId, CommonDto dto) throws CredentialNotFoundException, InvalidRequestException {
        String accessToken = vendorLineService.getCredentialValueByName(vendorLineId, Credentials.ACCESS_TOKEN);
        if (!accessToken.equals(dto.getAccessToken())) {
            throw new InvalidRequestException();
        }
    }

    public void verifyProductId(Integer vendorLineId, CommonDto dto) throws CredentialNotFoundException, InvalidRequestException {
        String accessToken = vendorLineService.getCredentialValueByName(vendorLineId, Credentials.PRODUCT_GROUP);
        if (!accessToken.equals(dto.getProductId())) {
            throw new InvalidRequestException();
        }
    }

    public void validateExternalGameSessionId(String externalGameSessionId) throws InvalidRequestException {
        if (!externalGameSessionId.matches("^[a-zA-Z0-9_-]+$")) {
            throw new InvalidRequestException();
        }
    }

    public <T> GameSession getGameSession(T dto)
            throws
            AuthenticationException,
            InvalidRequestException,
            NoSuchMethodException,
            InvocationTargetException,
            IllegalAccessException {

        GameSession gameSession;

        Method getExternalGameSessionIdMethod = dto.getClass().getMethod("getExternalGameSessionId");
        String externalGameSessionId = (String) getExternalGameSessionIdMethod.invoke(dto);

        if(externalGameSessionId == null || externalGameSessionId.equals("")) {
            Method getExternalId = dto.getClass().getMethod("getExternalId");
            gameSession = gameSessionService.getGameSessionByVendorPlayerUsername((String) getExternalId.invoke(dto));
        } else {
            // validate extern game session id
            this.validateExternalGameSessionId(externalGameSessionId);

            // Verify session token
            gameSession = gameSessionService.verifyToken(externalGameSessionId);
        }

        return gameSession;

    }

    public static Long getTimestamp() {
        return Instant.now().toEpochMilli();
    }

}
