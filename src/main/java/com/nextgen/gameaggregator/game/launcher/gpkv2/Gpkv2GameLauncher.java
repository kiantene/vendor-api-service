package com.nextgen.gameaggregator.game.launcher.gpkv2;

import com.nextgen.core.security.signature.SigningStrategyType;
import com.nextgen.gameaggregator.core.engine.game.url.AbstractGameLaunchHandler;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchHandler;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import com.nextgen.gameaggregator.entity.ga.VendorLineCredential;
import com.nextgen.gameaggregator.vendor.gpkv2.constant.Credentials;
import com.nextgen.gameaggregator.vendor.gpkv2.constant.EndPoints;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service(EndPoints.CLASS_NAME + GameLaunchHandler.NAME)
public class Gpkv2GameLauncher extends AbstractGameLaunchHandler<GameLaunchRequest, GameLaunchResponse> {

    public Gpkv2GameLauncher(VendorCredentialUtils credentialUtils) {
        super(credentialUtils, EndPoints.CLASS_NAME, GameLaunchResponse.class, SigningStrategyType.HMAC_SHA256_HEX);
    }


    @Override
    public void onSuccess(GameLaunchContext context, GameLaunchResponse response) {
        String gameUrl = response.getData().getUrl();
        context.setGameUrl(gameUrl);
    }

    @Override
    public String getBaseUrl(GameLaunchContext context) {
        VendorCredentialAccessor credentialAccessor = credentials(context.getVendorCredentials());
        VendorLineCredential urlSchemeCredential = credentialAccessor.get(Credentials.GAME_URL);
        return urlSchemeCredential.getValue();
    }

    @Override
    public String getPath(GameLaunchContext context) {
        return EndPoints.LAUNCH_GAME;
    }

    @Override
    public GameLaunchRequest buildRequestBody(GameLaunchContext context) {
        VendorCredentialAccessor accessor = credentials(context.getVendorCredentials());
        String cid = accessor.getValue(Credentials.CID);
        String country = accessor.getValue(Credentials.COUNTRY);
        String apiToken = accessor.getValue(Credentials.API_TOKEN);
        String providerId = accessor.getValue(Credentials.PROVIDER_ID);
        return GameLaunchRequest.builder()
                .api_token(apiToken)
                .cid(cid)
                .player_id(context.getVendorPlayerUsername())
                .provider(Integer.valueOf(providerId))
                .game(context.getVendorGameCode())
                .nickname(context.getVendorPlayerUsername())
                .currency(context.getVendorCurrencyCode())
                .player_token((context.getToken()))
                .balance(String.valueOf(BigDecimal.ZERO))
                .ip(context.getIpAddress())
                .country(country)
                .return_url(context.getLobbyUrl())
                .build();
    }

    @Override
    public MediaType getContentType() {
        return MediaType.APPLICATION_JSON;
    }

    @Override
    public Map<String, String> getHeaders(GameLaunchContext context, GameLaunchRequest requestObject) {
        VendorCredentialAccessor accessor = credentials(context.getVendorCredentials());
        String signKey = accessor.getValue(Credentials.SIGN_KEY);

        return Map.of(
                "X-GPK-SIGNATURE", sign(requestObject, signKey)
        );
    }

    @Override
    public boolean isSuccess(GameLaunchResponse gameLaunchResponse) {
        return gameLaunchResponse != null
                && gameLaunchResponse.getData() != null
                && gameLaunchResponse.getData().getUrl() != null;
    }
}
