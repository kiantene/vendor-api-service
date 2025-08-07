package com.nextgen.gameaggregator.vendor.inout.service;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.codec.binary.Hex;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

@Setter
@Getter
@Service
public class VendorService extends BaseVendorService {

    private final VendorLineService vendorLineService;
    private final GameSessionService gameSessionService;
    private final AgentPlayerService agentPlayerService;
    private final VendorGameService vendorGameService;
    private final ValidationService validationService;

    public VendorService(VendorLineService vendorLineService,
                         GameSessionService gameSessionService,
                         AgentPlayerService agentPlayerService,
                         VendorGameService vendorGameService,
                         ValidationService validationService) {
        this.vendorLineService = vendorLineService;
        this.gameSessionService = gameSessionService;
        this.agentPlayerService = agentPlayerService;
        this.vendorGameService = vendorGameService;
        this.validationService = validationService;
    }

    public static String hashHMACSha256(String data, String secret) {
        try {
            byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
            sha256Hmac.init(secretKey);
            byte[] hash = sha256Hmac.doFinal(dataBytes);
            return Hex.encodeHexString(hash);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    public GameSession checkGameSession(String traceId, String vendorPlayerUsername, String vendorGameCode, String vendorToken) throws VendorCurrencyNotSupportException, InvalidPlayerException, GameNotSupportedException {
        GameSession gameSession;
        try {
            gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(vendorPlayerUsername);
        } catch (AuthenticationException e) {
            gameSession = gameSessionService.generateNewSessionToken(vendorPlayerUsername);
            gameSessionService.updateByVendorGameCode(gameSession, vendorGameCode);
            gameSessionService.updateByVendorCurrencyId(gameSession);
            gameSession.setToken(traceId);
            gameSession.setVendorToken(vendorToken);
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

    public void doVerification(String currency, String gameMode, String vendorPlayerUsername, GameSession gameSession, String secretKey, String body, String xSign) throws
            AuthenticationException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            InvalidPlayerException {
        if (gameSession.getStatus() == 0) throw new AuthenticationException();

        // 1. Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // 2. Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // 3. Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

        // 4. Verify Currency
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), currency, AuthenticationException::new);

        // 5. Verify GameMode
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), gameMode, AuthenticationException::new);

        //6. Verify X-SIGNATURE
        ValidationUtils.isEquals(xSign, VendorService.hashHMACSha256(body, secretKey), AuthenticationException::new);

        validationService.validateEligibleBet(gameSession, vendorPlayerUsername);

    }
}
