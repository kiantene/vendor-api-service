package com.nextgen.gameaggregator.vendor.aviatorstudio.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.nextgen.gameaggregator.core.exception.GameSessionExpiredException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;

public class JwtUtil {
    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_IAT = "iat";

    static {
        // Register BouncyCastle provider if not already registered
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private JwtUtil() {}

    public static String generateJwt(String userId, String jwtSecret) {
        return generateSignedJwt(userId, jwtSecret);
    }

    private static String generateSignedJwt(String userId, String jwtSecret) {
        Algorithm algorithm = Algorithm.HMAC256(jwtSecret);

        return JWT.create()
                .withClaim(CLAIM_USER_ID, userId)
                .withClaim(CLAIM_IAT, System.currentTimeMillis())
                .sign(algorithm);
    }

    public static DecodedJWT verifyAndDecode(String jwtToken, String jwtSecret) throws JWTVerificationException {
        Algorithm algorithm = Algorithm.HMAC256(jwtSecret);

        // Decode token WITHOUT verifying claims like `iat`
        DecodedJWT decoded = JWT.decode(jwtToken);

        algorithm.verify(decoded); // Signature check only

        long issuedAtMillis = decoded.getClaim("iat").asLong();
        long nowMillis = System.currentTimeMillis();
        long twoDaysMillis = 2L * 24 * 60 * 60 * 1000;

        if (Math.abs(nowMillis - issuedAtMillis) > twoDaysMillis) {
            throw new GameSessionExpiredException();
        }
        return decoded;
    }

    public static String getUsername(String jwtToken) {
        DecodedJWT decodedJWT = JWT.decode(jwtToken);
        return decodedJWT.getClaim(CLAIM_USER_ID).asString();
    }
}
