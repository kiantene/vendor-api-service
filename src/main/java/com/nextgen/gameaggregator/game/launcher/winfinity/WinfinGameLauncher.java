package com.nextgen.gameaggregator.game.launcher.winfinity;

import com.nextgen.gameaggregator.core.engine.game.url.AbstractGameLaunchHandler;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchHandler;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.logging.LogContextService;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import com.nextgen.gameaggregator.game.launcher.winfinity.bearer.BearerTokenHolder;
import com.nextgen.gameaggregator.game.launcher.winfinity.bearer.BearerTokenService;
import com.nextgen.gameaggregator.game.launcher.winfinity.constant.EndPoints;
import com.nextgen.gameaggregator.game.launcher.winfinity.constant.Credentials;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service(EndPoints.CLASS_NAME + GameLaunchHandler.NAME)
public class WinfinGameLauncher extends AbstractGameLaunchHandler<GameLaunchRequest, GameLaunchResponse> {
    private static final String HEADER_AUTH = "Authorization";
    private static final String HEADER_BEARER = "Bearer ";

    private final BearerTokenService bearerTokenService;

    public WinfinGameLauncher(VendorCredentialUtils credentialUtils,
                              BearerTokenService bearerTokenService) {

        super(credentialUtils, EndPoints.CLASS_NAME, GameLaunchResponse.class);
        this.bearerTokenService = bearerTokenService;
    }

    @Override
    public String getPath(GameLaunchContext context) {
        return EndPoints.GAME_URL;
    }

    @Override
    public String getBaseUrl(GameLaunchContext context) {
        VendorCredentialAccessor credentialAccessor = credentials(context.getVendorCredentials());
        return credentialAccessor.getValue(Credentials.API_URL);
    }

    @Override
    public AbstractGameLaunchHandler<GameLaunchRequest, GameLaunchResponse> prepareLaunchRequest(GameLaunchContext context) {
        bearerTokenService.process(context);
        return super.prepareLaunchRequest(context);
    }

    @Override
    public GameLaunchRequest buildRequestBody(GameLaunchContext context) {
        VendorCredentialAccessor credentialAccessor = credentials(context.getVendorCredentials());
        String clientId = credentialAccessor.getValue(Credentials.CLIENT_ID);

        GameLaunchRequest.GameLaunchRequestBuilder gameLaunchRequest = GameLaunchRequest.builder()
                .user(buildUserData(clientId, context))
                .country(Credentials.COUNTRY)
                .currency(context.getVendorCurrencyCode())
                .device(context.getVendorPlatformCode())
                .ipAddress(context.getIpAddress());

        //if vendor game code is lobby, skip set table id
        if (!isLobby(context.getVendorGameCode())) {
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
        context.setVendorToken(response.getData().getMasterSessionId());
    }

    @Override
    public MediaType getContentType() {
        return MediaType.APPLICATION_JSON;
    }

    @Override
    public Map<String, String> getHeaders(GameLaunchContext context, GameLaunchRequest gameLaunchRequest) {
        String bearerToken = BearerTokenHolder.getToken();
        LogContext logContext = LogContextHolder.get();
        logContext.putApiHeader("bearer", bearerToken);

        //clear thread
        BearerTokenHolder.clear();
        return Map.of(
                HEADER_AUTH, HEADER_BEARER + bearerToken
        );
    }

    private GameLaunchRequest.User buildUserData(String clientId, GameLaunchContext context) {
        return GameLaunchRequest.User.builder()
                .partnerSiteId(clientId)
                .userId(context.getVendorPlayerUsername())
                .language(context.getVendorLanguageCode())
                .timeZoneOffset(Credentials.TIME_ZONE_OFFSET)
                .build();
    }

    private boolean isLobby(String vendorGameCode) {
        return "LOBBY".equalsIgnoreCase(vendorGameCode);
    }
}
