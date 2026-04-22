package com.nextgen.gameaggregator.game.launcher.hp100;

import com.nextgen.gameaggregator.core.engine.game.url.AbstractGameLaunchHandler;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import com.nextgen.gameaggregator.vendor.hp100.config.HP100VendorConfig;
import com.nextgen.gameaggregator.vendor.hp100.constant.Credentials;
import com.nextgen.gameaggregator.vendor.hp100.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.hp100.constant.Platforms;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.net.URI;


@Component
public class Hp100GameLauncher extends AbstractGameLaunchHandler<GameLaunchRequest, GameLaunchResponse> {
    protected Hp100GameLauncher(VendorCredentialUtils credentialUtils) {
        super(credentialUtils, HP100VendorConfig.CLASS_NAME, GameLaunchResponse.class);
    }

    @Override
    public MediaType getContentType() {
        return MediaType.APPLICATION_JSON;
    }

    @Override
    public void onSuccess(GameLaunchContext context, GameLaunchResponse response) {
        context.setGameUrl(response.getGameUrl());
    }

    @Override
    public String getBaseUrl(GameLaunchContext gameLaunchContext) {
        VendorCredentialAccessor accessor = credentials(gameLaunchContext.getVendorCredentials());
        return accessor.getValue(Credentials.API_URL);
    }

    @Override
    public String getPath(GameLaunchContext gameLaunchContext) {
        return Endpoints.GAME_URL;
    }

    @Override
    public GameLaunchRequest buildRequestBody(GameLaunchContext gameLaunchContext) {
        VendorCredentialAccessor accessor = credentials(gameLaunchContext.getVendorCredentials());
        return GameLaunchRequest.builder()
                .sessionId(gameLaunchContext.getToken())
                .isDemo(false)
                .isMobile(gameLaunchContext.getVendorPlatformCode().equals(Platforms.H5))
                .partnerId(accessor.getValue(Credentials.PARTNER_ID))
                .secretKey(accessor.getValue(Credentials.SECRET_KEY))
                .gameId(gameLaunchContext.getVendorGameCode())
                .build();
    }

    @Override
    public boolean isSuccess(GameLaunchResponse gameLaunchResponse) {
        if (gameLaunchResponse == null || gameLaunchResponse.getGameUrl() == null) {
            return false;
        }

        // Add URL validation
        try {
            new URI(gameLaunchResponse.getGameUrl()).toURL();
            return true;
        } catch (Exception e) {
            return false;
        }

    }

}
