package com.nextgen.gameaggregator.game.launcher.winfinity;

import com.nextgen.gameaggregator.core.engine.game.url.AbstractGameLaunchHandler;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchHandler;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import com.nextgen.gameaggregator.game.launcher.winfinity.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.winfinity.constant.Credentials;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.util.*;

@Service(EndPoints.CLASSS_NAME + GameLaunchHandler.NAME)
public class WinfinGameLauncher extends AbstractGameLaunchHandler<GameLaunchRequest, GameLaunchResponse> {

    private String token = "";
    private final GetBearerTokenService getBearerTokenService;

    public WinfinGameLauncher(VendorCredentialUtils credentialUtils, GetBearerTokenService getBearerTokenService) {
        super(credentialUtils, EndPoints.CLASSS_NAME, GameLaunchResponse.class);
        this.getBearerTokenService = getBearerTokenService;
    }

    @Override
    public String getPath(GameLaunchContext context) {
        return "";
    }

    @Override
    public String getBaseUrl(GameLaunchContext context) {
        VendorCredentialAccessor credentialAccessor = credentials(context.getVendorCredentials());
        return credentialAccessor.getValue(Credentials.API_URL) + EndPoints.GAME;
    }

    @Override
    public AbstractGameLaunchHandler<GameLaunchRequest, GameLaunchResponse> prepareLaunchRequest(GameLaunchContext context) {
        getBearerTokenService.process(context);
        token = BearerTokenHolder.getBody();
        return super.prepareLaunchRequest(context);
    }

    @Override
    public GameLaunchRequest buildRequestBody(GameLaunchContext context) {
        VendorCredentialAccessor credentialAccessor = credentials(context.getVendorCredentials());
        String clientId = credentialAccessor.getValue(Credentials.CLIENT_ID);

        GameLaunchRequest.User user = GameLaunchRequest.User.builder()
                .partnerSiteId(clientId)
                .userId(context.getVendorPlayerUsername())
                .language(context.getVendorLanguageCode())
                .timeZoneOffset("00:00:00")
                .build();
        GameLaunchRequest.GameLaunchRequestBuilder gameLaunchRequest = GameLaunchRequest.builder()
                .user(user)
                .country("DE")
                .currency(context.getVendorCurrencyCode())
                .device(context.getVendorPlatformCode())
                .ipAddress(context.getIpAddress());

        //if vendor game code is lobby, skip set table id
        if (!"LOBBY".equalsIgnoreCase(context.getVendorGameCode())) {
            gameLaunchRequest.tableId(context.getVendorGameCode());
        }
        return gameLaunchRequest.build();
    }

    @Override
    public boolean isSuccess(GameLaunchResponse response) {
        return response.getData() != null && !response.getData().getFrameUrl().isBlank();
    }

    @Override
    public void onSuccess(GameLaunchContext context, GameLaunchResponse response) {
        context.setGameUrl(response.getData().getFrameUrl());
    }

    @Override
    public MediaType getContentType() {
        return MediaType.APPLICATION_JSON;
    }

    @Override
    public Map<String, String> getHeaders(GameLaunchContext context, GameLaunchRequest gameLaunchRequest) {

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + token);
        BearerTokenHolder.clear();
        headers.put("Content-Type", "application/json");
        return headers;
    }
}
