package com.nextgen.gameaggregator.game.launcher.winfinity;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.core.engine.game.url.AbstractGameLaunchHandler;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import com.nextgen.gameaggregator.game.launcher.winfinity.constant.Credentials;
import com.nextgen.gameaggregator.game.launcher.winfinity.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.winfinity.service.VendorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Component
public class GetBearerTokenHandler extends AbstractGameLaunchHandler<GetBearerTokenRequest, GetBearerTokenResponse> {

    @Autowired
    private VendorService vendorService;

    protected GetBearerTokenHandler(VendorCredentialUtils credentialUtils) {
        super(credentialUtils, EndPoints.CLASSS_NAME + "GetBearerTokenHandler", GetBearerTokenResponse.class);
    }

    @Override
    public void onSuccess(GameLaunchContext context, GetBearerTokenResponse response) {
        BearerTokenHolder.setBody(response.getAccessToken());
    }

    @Override
    public String getBaseUrl(GameLaunchContext gameLaunchContext) {
        VendorCredentialAccessor credentialAccessor = credentials(gameLaunchContext.getVendorCredentials());
        return credentialAccessor.getValue(Credentials.API_URL);
    }

    @Override
    public String getPath(GameLaunchContext gameLaunchContext) {
        return EndPoints.TOKEN;
    }

    @Override
    public GetBearerTokenRequest buildRequestBody(GameLaunchContext gameLaunchContext) {
        VendorCredentialAccessor credentialAccessor = credentials(gameLaunchContext.getVendorCredentials());
        String clientId = credentialAccessor.getValue(Credentials.CLIENT_ID);
        String clientSecret = credentialAccessor.getValue(Credentials.CLIENT_SECRET);

        return GetBearerTokenRequest.builder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .grantType("client_credentials")
                .scope(new String[]{"partner_client.call"})
                .build();
    }

    @Override
    public MediaType getContentType() {
        return MediaType.APPLICATION_JSON;
    }

    @Override
    public boolean isSuccess(GetBearerTokenResponse getBearerTokenResponse) {
        return getBearerTokenResponse.getAccessToken() != null && !getBearerTokenResponse.getAccessToken().isBlank();
    }

    @Override
    public Map<String, String> getHeaders(GameLaunchContext context, GetBearerTokenRequest getBearerTokenRequest) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return headers;
    }
}
