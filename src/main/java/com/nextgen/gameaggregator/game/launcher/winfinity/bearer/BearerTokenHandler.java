package com.nextgen.gameaggregator.game.launcher.winfinity.bearer;

import com.nextgen.gameaggregator.core.engine.game.url.AbstractGameLaunchHandler;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import com.nextgen.gameaggregator.game.launcher.winfinity.constant.Credentials;
import com.nextgen.gameaggregator.game.launcher.winfinity.constant.EndPoints;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class BearerTokenHandler extends AbstractGameLaunchHandler<BearerTokenRequest, BearerTokenResponse> {

    protected BearerTokenHandler(VendorCredentialUtils credentialUtils) {
        super(credentialUtils, EndPoints.CLASS_NAME + "BearerTokenHandler", BearerTokenResponse.class);
    }

    @Override
    public void onSuccess(GameLaunchContext context, BearerTokenResponse response) {
        BearerTokenHolder.setToken(response.getAccessToken());
    }

    @Override
    public String getBaseUrl(GameLaunchContext gameLaunchContext) {
        VendorCredentialAccessor accessor = credentials(gameLaunchContext.getVendorCredentials());
        return accessor.getValue(Credentials.API_URL);
    }

    @Override
    public String getPath(GameLaunchContext gameLaunchContext) {
        return EndPoints.TOKEN;
    }

    @Override
    public BearerTokenRequest buildRequestBody(GameLaunchContext gameLaunchContext) {
        VendorCredentialAccessor accessor = credentials(gameLaunchContext.getVendorCredentials());
        String clientId = accessor.getValue(Credentials.CLIENT_ID);
        String clientSecret = accessor.getValue(Credentials.CLIENT_SECRET);

        return BearerTokenRequest.builder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .grantType(Credentials.BEARER_GRANT_TYPE)
                .scope(Credentials.BEARER_SCOPE)
                .build();
    }

    @Override
    public MediaType getContentType() {
        return MediaType.APPLICATION_JSON;
    }

    @Override
    public boolean isSuccess(BearerTokenResponse bearerTokenResponse) {
        return bearerTokenResponse.getAccessToken() != null && !bearerTokenResponse.getAccessToken().isBlank();
    }
}
