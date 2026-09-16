package com.nextgen.gameaggregator.game.launcher.casinogate;

import com.nextgen.gameaggregator.core.engine.game.url.AbstractGameLaunchHandler;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchHandler;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import com.nextgen.gameaggregator.util.GeoIpUtil;
import com.nextgen.gameaggregator.vendor.casinogate.constant.Credentials;
import com.nextgen.gameaggregator.vendor.casinogate.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.casinogate.constant.Platforms;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

@Service(Endpoints.CLASS_NAME + GameLaunchHandler.NAME)
public class CasinoGateGameLauncher extends AbstractGameLaunchHandler<GameLaunchRequest, GameLaunchResponse> {
    protected CasinoGateGameLauncher(VendorCredentialUtils credentialUtils) {
        super(credentialUtils, Endpoints.CLASS_NAME, GameLaunchResponse.class);
    }

    @Override
    public void onSuccess(GameLaunchContext context, GameLaunchResponse response) {
        context.setGameUrl(response.getUrl());
    }

    @Override
    public String getBaseUrl(GameLaunchContext gameLaunchContext) {
        VendorCredentialAccessor credentialAccessor = credentials(gameLaunchContext.getVendorCredentials());
        return credentialAccessor.get(Credentials.API_URL).getValue();
    }

    @Override
    public String getPath(GameLaunchContext gameLaunchContext) {
        return Endpoints.LAUNCH_PATH;
    }

    @Override
    public GameLaunchRequest buildRequestBody(GameLaunchContext context) {
        VendorCredentialAccessor credentialAccessor = credentials(context.getVendorCredentials());

        return GameLaunchRequest.builder()
                .buffer(Collections.emptyMap())
                .cashierUrl(context.getLobbyUrl())
                .casinoId(credentialAccessor.getValue(Credentials.CASINO_ID))
                .countryCode(GeoIpUtil.getCountryCode(context.getIpAddress()))
                .currency(context.getVendorCurrencyCode())
                .demo(false)
                .gameId(context.getVendorGameCode())
                .ipAddress(context.getIpAddress())
                .mobile(context.getPlatformId().equals(1) ? Platforms.H5 : Platforms.WEB)
                .language(context.getVendorLanguageCode())
                .lobbyUrl(context.getLobbyUrl())
                .token(context.getToken())
                .userId(context.getVendorPlayerUsername())
                .build();
    }

    @Override
    public MediaType getContentType() {
        return MediaType.APPLICATION_JSON;
    }

    @Override
    public boolean isSuccess(GameLaunchResponse gameLaunchResponse) {
        return gameLaunchResponse != null && gameLaunchResponse.getUrl() != null;
    }

    @Override
    public Map<String, String> getHeaders(GameLaunchContext context, GameLaunchRequest requestObject) {
        VendorCredentialAccessor credentialAccessor = credentials(context.getVendorCredentials());
        String username = credentialAccessor.getValue(Credentials.USERNAME);
        String password = credentialAccessor.getValue(Credentials.PASSWORD);
        String credentials = username + ":" + password;
        String encodedCredentials = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        return Map.of(HttpHeaders.AUTHORIZATION, "Basic " + encodedCredentials);
    }
}
