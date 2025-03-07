package com.nextgen.gameaggregator.vendor.smartsoft.service;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.GameSessionService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@Slf4j
@Data
public class VendorService extends BaseVendorService {
    @Autowired
    private GameSessionService gameSessionService;

    public static String md5Generator(String input) {
        return DigestUtils.md5Hex(input);
    }

    public static String signatureGenerator(String secretKey, String requestMethod, String requestPayload) {
        return md5Generator(secretKey + "|" + requestMethod + "|" + requestPayload);
    }

    public GameSession preCheckGameSessionToken(String token) throws AuthenticationException {
        GameSession gameSession = null;
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

}
