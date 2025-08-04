package com.nextgen.gameaggregator.vendor.aviatorstudio.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.Credentials;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Setter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Enumeration;

@Setter
@Getter
@Service
public class VendorService extends BaseVendorService {
    public static final String CLAIM_USER_ID = "userId";

    private final VendorLineService vendorLineService;
    private final GameSessionService gameSessionService;

    public VendorService(VendorLineService vendorLineService,
                         GameSessionService gameSessionService) {
        this.vendorLineService = vendorLineService;
        this.gameSessionService = gameSessionService;
    }

    public static <T> void doValidation(T validationObject) throws InvalidRequestException {
        ValidationUtils.validateRequest(validationObject);
    }

    public static String jwtGetUserId(String jwtToken) {
        DecodedJWT decodedJWT = JWT.decode(jwtToken);
        return decodedJWT.getClaim(CLAIM_USER_ID).asString();
    }

    public DecodedJWT decodeJWT(String jwtToken, int vendorLineId) throws CredentialNotFoundException {
        String jwtSecret = vendorLineService.getCredentialValueByName(vendorLineId, Credentials.JWT_SECRET);
        Algorithm algorithm = Algorithm.HMAC256(jwtSecret);

        // Decode token WITHOUT verifying claims like `iat`
        DecodedJWT decoded = JWT.decode(jwtToken);

        algorithm.verify(decoded); // Signature check only

        return decoded;
    }

    public void verifyJWT(String jwtAuth, int vendorLineId, String vendorPlayerUsername) throws AuthenticationException, CredentialNotFoundException {
        DecodedJWT decodedJWT = decodeJWT(jwtAuth, vendorLineId);

        //Verify username
        ValidationUtils.isEquals(vendorPlayerUsername, decodedJWT.getClaim(CLAIM_USER_ID).asString(), AuthenticationException::new);

        long issuedAtMillis = decodedJWT.getClaim("iat").asLong();
        long nowMillis = System.currentTimeMillis();
        long twoDaysMillis = 2L * 24 * 60 * 60 * 1000;

        if (Math.abs(nowMillis - issuedAtMillis) > twoDaysMillis) {
            throw new AuthenticationException();
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
}
