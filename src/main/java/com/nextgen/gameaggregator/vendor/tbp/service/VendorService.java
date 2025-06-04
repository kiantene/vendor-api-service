package com.nextgen.gameaggregator.vendor.tbp.service;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.tbp.constant.Credentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class VendorService extends BaseVendorService {
    private final VendorLineService vendorLineService;
    private final GameSessionService gameSessionService;

    public VendorService(VendorLineService vendorLineService,
                         GameSessionService gameSessionService) {
        this.vendorLineService = vendorLineService;
        this.gameSessionService = gameSessionService;
    }

    public void validate(String usernameDto, String passwordDto, String playerIdDto, String currencyDto, String sessionIdDto, GameSession gameSession)
            throws AuthenticationException, CredentialNotFoundException {

        // 1. Verify Username
        String username = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.USERNAME);
        ValidationUtils.isEquals(username, usernameDto, AuthenticationException::new);

        // 2. Verify Password
        String password = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.TOKEN);
        ValidationUtils.isEquals(password, passwordDto, AuthenticationException::new);

        // 3. Verify PlayerId
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), playerIdDto, AuthenticationException::new);

        // 4. Verify Currency
        ValidationUtils.isEquals(gameSession.getCurrencyCode(), currencyDto, AuthenticationException::new);

        // 5. Verify SessionId
        ValidationUtils.isEquals(gameSession.getVendorToken(), sessionIdDto, AuthenticationException::new);
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
}
