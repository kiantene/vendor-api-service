package com.nextgen.gameaggregator.vendor.avatarux.service;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.InvalidPlayerException;
import com.nextgen.gameaggregator.exception.VendorCurrencyNotSupportException;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.GameSessionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;

@Service
@Slf4j
public class VendorService extends BaseVendorService {
    private static final String HASH_ALGORITHM = "HmacSHA256";
    private final GameSessionService gameSessionService;

    public VendorService(GameSessionService gameSessionService) {
        this.gameSessionService = gameSessionService;
    }

    public static String generateHash(String apiSecret, String input) throws AuthenticationException {
        try {
            // Create a new secret key based on the given API secret
            SecretKeySpec keySpec = new SecretKeySpec(apiSecret.getBytes(), HASH_ALGORITHM);

            // Generate a message authentication code (MAC) from the input and key
            Mac mac = Mac.getInstance(HASH_ALGORITHM);
            mac.init(keySpec);
            byte[] hashBytes = mac.doFinal(input.getBytes());

            // Convert the MAC to a hexadecimal string format
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error generating signature : {}", e.getMessage());
            throw new AuthenticationException();
        }
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

    public GameSession checkGameSession(String traceId, String userName) throws VendorCurrencyNotSupportException, InvalidPlayerException {
        GameSession gameSession;
        try {
            gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(userName);
        } catch (AuthenticationException authenticationException) {
            gameSession = gameSessionService.generateNewSessionToken(userName);
            gameSessionService.updateByVendorCurrencyId(gameSession);
            gameSession.setToken(traceId);
            gameSession.setVendorToken(traceId);
        }
        return gameSession;
    }
}
