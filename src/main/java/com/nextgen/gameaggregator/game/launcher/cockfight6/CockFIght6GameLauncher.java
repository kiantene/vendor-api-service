package com.nextgen.gameaggregator.game.launcher.cockfight6;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.core.security.signature.SigningStrategyType;
import com.nextgen.gameaggregator.core.engine.game.url.AbstractGameLaunchHandler;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchHandler;
import com.nextgen.gameaggregator.core.exception.GameLaunchException;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import com.nextgen.gameaggregator.entity.ga.VendorLineCredential;
import com.nextgen.gameaggregator.vendor.cockfight6.constant.Credentials;
import com.nextgen.gameaggregator.vendor.cockfight6.constant.EndPoints;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Map;

@Service(EndPoints.CLASS_NAME + GameLaunchHandler.NAME)
public class CockFIght6GameLauncher extends AbstractGameLaunchHandler<GameLaunchRequest, GameLaunchResponse> {
    protected CockFIght6GameLauncher(VendorCredentialUtils credentialUtils) {
        super(credentialUtils, EndPoints.CLASS_NAME, GameLaunchResponse.class, SigningStrategyType.HMAC_SHA256_BASE64);
    }

    @Override
    public MediaType getContentType() {
        return MediaType.APPLICATION_JSON;
    }

    @Override
    public void onSuccess(GameLaunchContext context, GameLaunchResponse response) {
        context.setGameUrl(response.getData().getEntry());
    }

    @Override
    public String getBaseUrl(GameLaunchContext gameLaunchContext) {
        VendorCredentialAccessor credentialAccessor = credentials(gameLaunchContext.getVendorCredentials());
        VendorLineCredential apiUrl = credentialAccessor.get(Credentials.API_URL);
        return apiUrl.getValue();
    }

    @Override
    public String getPath(GameLaunchContext gameLaunchContext) {
        return EndPoints.GAME_URL;
    }

    @Override
    public GameLaunchRequest buildRequestBody(GameLaunchContext gameLaunchContext) {
        VendorCredentialAccessor accessor = credentials(gameLaunchContext.getVendorCredentials());
        String agent = accessor.getValue(Credentials.AGENT);

        return GameLaunchRequest.builder()
                .agent(agent)
                .external_player_id(gameLaunchContext.getVendorPlayerUsername())
                .login_device(Integer.valueOf(gameLaunchContext.getVendorPlatformCode()))
                .build();
    }

    @Override
    public Map<String, String> getHeaders(GameLaunchContext context, GameLaunchRequest requestObject) {
        ObjectMapper objectMapper = new ObjectMapper();
        VendorCredentialAccessor accessor = credentials(context.getVendorCredentials());
        String sn = accessor.getValue(Credentials.SN);
        String secretKey = accessor.getValue(Credentials.SECRETKEY);
        long ts = System.currentTimeMillis() / 1000;

        try {
            return Map.of(
                    "sn", sn,
                    "ts", String.valueOf(ts),
                    "signature", sign(sn + "_" + ts + "_" + objectMapper.writeValueAsString(requestObject), secretKey));
        } catch (JsonProcessingException e) {
            throw new GameLaunchException("Failed to serialize GameLaunchRequest", e);
        }
    }

    @Override
    public boolean isSuccess(GameLaunchResponse gameLaunchResponse) {
        if (gameLaunchResponse == null || gameLaunchResponse.getCode() != 0) {
            return false;
        }
        try {
            new URI(gameLaunchResponse.getData().getEntry()).toURL();
            return true;
        } catch (Exception e) {
            return false;
        }

    }
}
