package com.nextgen.gameaggregator.game.launcher.aviatorstudio;


import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchHandler;
import com.nextgen.gameaggregator.core.engine.game.url.QueryStringUrlGameLauncher;
import com.nextgen.gameaggregator.entity.ga.VendorLineCredential;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.Credentials;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.aviatorstudio.service.VendorService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service(EndPoints.CLASS_NAME + GameLaunchHandler.NAME)
public class ASGameLauncher extends QueryStringUrlGameLauncher<LaunchRequestPayload> {

    @Override
    public String getVendorClassName() {
        return EndPoints.CLASS_NAME;
    }

    @Override
    public String getBaseUrl(GameLaunchContext context) {
        VendorLineCredential urlSchemeCredential = getRequiredCredential(context.getVendorCredentials(), Credentials.API_URL);
        return urlSchemeCredential.getValue();
    }

    @Override
    public String getPath() {
        return "";
    }

    @Override
    public LaunchRequestPayload onPrepareRequestBody(GameLaunchContext context) {
        Map<String, VendorLineCredential> credentialMap = context.getVendorCredentials();
        String publicKey = getRequiredCredentialValue(credentialMap, Credentials.PUBLIC_KEY);
        //String jwtToken = getRequiredCredentialValue(credentialMap, Credentials.JWT_SECRET);
        String userid = context.getVendorPlayerUsername();
        String sessionId = context.getToken();
        String providerId = getRequiredCredentialValue(credentialMap, Credentials.PROVIDER_ID);
        String currency = context.getVendorCurrencyCode();
        String language = context.getVendorLanguageCode();
        String vendorGameCode = context.getVendorGameCode();
        String token;

        try {
            //generate JWT token
            token = VendorService.generateJWT(userid, sessionId, providerId, publicKey);
        } catch (Exception exception) {
            throw new RuntimeException(exception.getMessage());
        }

        return GameLaunchRequest.builder()
                .token(token)
                .providerId(providerId)
                .currency(currency)
                .language(language)
                .gameId(vendorGameCode)
                .build();
    }
}

