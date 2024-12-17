package com.nextgen.gameaggregator.vendor.poker365.service;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class VendorService extends BaseVendorService {

    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private VendorGameCodeService vendorGameCodeService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private HttpService httpService;

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

        if (externalGameSessionId == null || externalGameSessionId.isEmpty()) {
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
}
