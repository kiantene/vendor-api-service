package com.nextgen.gameaggregator.vendor.smartsoft.service;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.GameNotSupportedException;
import com.nextgen.gameaggregator.exception.InvalidPlayerException;
import com.nextgen.gameaggregator.exception.VendorCurrencyNotSupportException;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.GameSessionService;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import java.util.Enumeration;
import java.util.Objects;

@Service
public class VendorService extends BaseVendorService {

    private final GameSessionService gameSessionService;

    public VendorService(GameSessionService gameSessionService) {
        this.gameSessionService = gameSessionService;
    }

    public static String md5Generator(String input) {
        return DigestUtils.md5Hex(input);
    }

    public static String signatureGenerator(String secretKey, String requestMethod, String requestPayload) {
        return md5Generator(secretKey + "|" + requestMethod + "|" + requestPayload);
    }

    public GameSession checkGameSession(String traceId, String userName, String vendorGameCode) throws VendorCurrencyNotSupportException, InvalidPlayerException, GameNotSupportedException {
        GameSession gameSession;
        try {
            gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(userName);
            this.verifyAndRegenerateNewVendorGameCodeForGameSession(vendorGameCode, gameSession);
        } catch (AuthenticationException authenticationException) {
            gameSession = gameSessionService.generateNewSessionToken(userName);
            gameSessionService.updateByVendorGameCode(gameSession, vendorGameCode);
            gameSessionService.updateByVendorCurrencyId(gameSession);
            gameSession.setToken(traceId);
            gameSession.setVendorToken(traceId);
        }
        return gameSession;
    }

    public GameSession preCheckGameSessionToken(String token) throws AuthenticationException {
        GameSession gameSession;
        try {
            gameSession = gameSessionService.verifyVendorToken(token);
        } catch (AuthenticationException authenticationException) {
            gameSession = gameSessionService.verifyToken(token);
        }

        if (Objects.isNull(gameSession.getVendorToken())) {
            gameSession.setVendorToken(gameSession.getToken());
            gameSessionService.updateSession(gameSession);
        }

        return gameSession;
    }

    public String getHeaders(HttpServletRequest request) {
        Enumeration<String> headerNames = request.getHeaderNames();
        StringBuilder headersString = new StringBuilder();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            headersString.append(headerName)
                    .append(":")
                    .append(headerValue)
                    .append("\n");
        }
        return headersString.toString();
    }
}
