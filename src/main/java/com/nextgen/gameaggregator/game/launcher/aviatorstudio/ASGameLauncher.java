package com.nextgen.gameaggregator.game.launcher.aviatorstudio;

import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchHandler;
import com.nextgen.gameaggregator.core.engine.game.url.QueryStringUrlGameLauncher;
import com.nextgen.gameaggregator.core.util.JwtUtil;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.Credentials;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.EndPoints;
import org.springframework.stereotype.Service;

@Service(EndPoints.CLASS_NAME + GameLaunchHandler.NAME)
public class ASGameLauncher extends QueryStringUrlGameLauncher<GameLaunchRequest> {

    protected ASGameLauncher(VendorCredentialUtils credentialUtils) {
        super(credentialUtils, EndPoints.CLASS_NAME);
    }

    @Override
    public String getBaseUrl(GameLaunchContext context) {
        VendorCredentialAccessor credentialAccessor = credentials(context.getVendorCredentials());
        return credentialAccessor.getValue(Credentials.API_URL);
    }

    @Override
    public String getPath(GameLaunchContext gameLaunchContext) {
        return "";
    }

    @Override
    public GameLaunchRequest buildRequestBody(GameLaunchContext context) {
        VendorCredentialAccessor accessor = credentials(context.getVendorCredentials());
        String publicKey = accessor.getValue(Credentials.PUBLIC_KEY);
        String jwtSecret = accessor.getValue(Credentials.JWT_SECRET);
        String userid = context.getVendorPlayerUsername();

        try {
            //generate JWT token
            String token = JwtUtil.generateJwt(userid, jwtSecret, publicKey);
            return GameLaunchRequest.builder()
                    .token(token)
                    .providerId(accessor.getValue(Credentials.PROVIDER_ID))
                    .currency(context.getVendorCurrencyCode())
                    .language(context.getVendorLanguageCode())
                    .gameId(context.getVendorGameCode())
                    .build();

        } catch (Exception exception) {
            throw new RuntimeException(exception.getMessage());
        }
    }
}
