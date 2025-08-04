package com.nextgen.gameaggregator.game.launcher.aviatorstudio;


import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchHandler;
import com.nextgen.gameaggregator.core.engine.game.url.QueryStringUrlGameLauncher;
import com.nextgen.gameaggregator.core.util.JwtUtil;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import com.nextgen.gameaggregator.entity.ga.VendorLineCredential;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.Credentials;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.EndPoints;
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
        String providerId = credentialAccessor.getValue(Credentials.PROVIDER_ID);
        String userid = context.getVendorPlayerUsername();
        String currency = context.getVendorCurrencyCode();
        String language = context.getVendorLanguageCode();
        String vendorGameCode = context.getVendorGameCode();

        try {
            //generate JWT token
            String token = JwtUtil.generateJwt(userid, jwtSecret, publicKey);

            return GameLaunchRequest.builder()
                    .token(token)
                    .providerId(providerId)
                    .currency(currency)
                    .language(language)
                    .gameId(vendorGameCode)
                    .build();

        } catch (Exception exception) {
            throw new RuntimeException(exception.getMessage());
        }
    }
}
