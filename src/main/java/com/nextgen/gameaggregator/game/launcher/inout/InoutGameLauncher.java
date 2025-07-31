package com.nextgen.gameaggregator.game.launcher.inout;


import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchHandler;
import com.nextgen.gameaggregator.core.engine.game.url.QueryStringUrlGameLauncher;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import com.nextgen.gameaggregator.entity.ga.VendorLineCredential;
import com.nextgen.gameaggregator.vendor.inout.constant.Credentials;
import com.nextgen.gameaggregator.vendor.inout.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.inout.service.VendorService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service(EndPoints.CLASS_NAME + GameLaunchHandler.NAME)
public class InoutGameLauncher extends QueryStringUrlGameLauncher<LaunchRequestPayload> {

    protected InoutGameLauncher(VendorCredentialUtils credentialUtils) {
        super(credentialUtils, EndPoints.CLASS_NAME);
    }

    @Override
    public String getBaseUrl(GameLaunchContext context) {
        VendorCredentialAccessor credentialAccessor = credentials(context.getVendorCredentials());
        VendorLineCredential urlSchemeCredential = credentialAccessor.get(Credentials.API_FRAME_URL);
        return urlSchemeCredential.getValue();
    }

    @Override
    public String getPath(GameLaunchContext gameLaunchContext) {
        return "";
    }

    @Override
    public LaunchRequestPayload buildRequestBody(GameLaunchContext context) {
        VendorCredentialAccessor credentialAccessor = credentials(context.getVendorCredentials());
        String secretKey = credentialAccessor.getValue(Credentials.SECRET_KEY);
        String vendorGameCode = context.getVendorGameCode();
        String operatorId = credentialAccessor.getValue(Credentials.OPERATOR_ID);
        String currency = context.getVendorCurrencyCode();
        String authToken = context.getToken();
        String language = context.getVendorLanguageCode();
        String lobbyUrl = context.getLobbyUrl();
        String subId = credentialAccessor.getValue(Credentials.NAME);
        boolean isDemoPlay = false;
        String token;
        try {
            token = VendorService.hashHMACSha256(operatorId + ":" + subId, secretKey);
        } catch (Exception exception) {
            throw new RuntimeException(exception.getMessage());
        }

        return GameLaunchRequest.builder()
                .gameMode(vendorGameCode)
                .aggregatorId(UUID.fromString(operatorId))
                .currency(currency)
                .authToken(authToken)
                .lang(language)
                .themeId(UUID.fromString(operatorId))
                .lobbyUrl(lobbyUrl)
                .subId(subId)
                .isDemoPlay(isDemoPlay)
                .token(token)
                .build();
    }
}

