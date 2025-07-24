package com.nextgen.gameaggregator.game.launcher.aviatorstudio;


import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchHandler;
import com.nextgen.gameaggregator.core.engine.game.url.QueryStringUrlGameLauncher;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import com.nextgen.gameaggregator.entity.ga.VendorLineCredential;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.Credentials;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.aviatorstudio.service.VendorService;
import org.springframework.stereotype.Service;

@Service(EndPoints.CLASS_NAME + GameLaunchHandler.NAME)
public class ASGameLauncher extends QueryStringUrlGameLauncher<LaunchRequestPayload> {

    protected ASGameLauncher(VendorCredentialUtils credentialUtils) {
        super(credentialUtils, EndPoints.CLASS_NAME);
    }

    @Override
    public String getBaseUrl(GameLaunchContext context) {
        VendorCredentialAccessor credentialAccessor = credentials(context.getVendorCredentials());
        VendorLineCredential urlSchemeCredential = credentialAccessor.get(Credentials.API_URL);
        return urlSchemeCredential.getValue();
    }

    @Override
    public String getPath(GameLaunchContext gameLaunchContext) {
        return "";
    }

    @Override
    public LaunchRequestPayload buildRequestBody(GameLaunchContext context) {
        VendorCredentialAccessor credentialAccessor = credentials(context.getVendorCredentials());
        String publicKey = credentialAccessor.getValue(Credentials.PUBLIC_KEY);
        String jwtSecret = credentialAccessor.getValue(Credentials.JWT_SECRET);
        String userid = context.getVendorPlayerUsername();
        String providerId = credentialAccessor.getValue(Credentials.PROVIDER_ID);
        String currency = context.getVendorCurrencyCode();
        String language = context.getVendorLanguageCode();
        String vendorGameCode = context.getVendorGameCode();
        String token;

        try {
            //generate JWT token
            token = VendorService.generateJWT(userid, jwtSecret, publicKey);
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

