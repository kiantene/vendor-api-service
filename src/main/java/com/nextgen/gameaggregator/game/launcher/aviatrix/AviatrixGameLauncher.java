package com.nextgen.gameaggregator.game.launcher.aviatrix;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchHandler;
import com.nextgen.gameaggregator.core.engine.game.url.AbstractGameLaunchHandler;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import com.nextgen.gameaggregator.vendor.aviatrix.constant.Credentials;
import com.nextgen.gameaggregator.vendor.aviatrix.constant.EndPoints;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Map;

@Service(EndPoints.CLASS_NAME + GameLaunchHandler.NAME)
public class AviatrixGameLauncher extends AbstractGameLaunchHandler<GameLaunchRequest, GameLaunchResponse> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public AviatrixGameLauncher(VendorCredentialUtils credentialUtils) {
        super(credentialUtils, EndPoints.CLASS_NAME, GameLaunchResponse.class);
    }

    @Override
    public String getBaseUrl(GameLaunchContext context) {
        return credentials(context.getVendorCredentials()).getValue(Credentials.API_URL);
    }

    @Override
    public String getPath(GameLaunchContext context) {
        return EndPoints.GAME_URL;
    }

    @Override
    public MediaType getContentType() {
        return MediaType.APPLICATION_JSON;
    }

    @Override
    public GameLaunchRequest buildRequestBody(GameLaunchContext context) {
        VendorCredentialAccessor accessor = credentials(context.getVendorCredentials());
        return GameLaunchRequest.builder()
                .cid(accessor.getValue(Credentials.CID))
                .productId(context.getVendorGameCode())
                .sessionToken(context.getToken())
                .demo(false)
                .lang(context.getVendorLanguageCode())
                .lobbyUrl(context.getLobbyUrl())
                .build();
    }

    @Override
    public Map<String, String> getHeaders(GameLaunchContext context, GameLaunchRequest requestBody) {
        String key = credentials(context.getVendorCredentials()).getValue(Credentials.KEY);
        try {
            Mac mac = Mac.getInstance("HmacMD5");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacMD5"));
            String signature = Base64.getEncoder().encodeToString(
                    mac.doFinal(OBJECT_MAPPER.writeValueAsBytes(requestBody)));
            return Map.of(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE,
                    "X-Auth-Signature", signature);
        } catch (GeneralSecurityException | JsonProcessingException e) {
            throw new IllegalStateException("Unable to sign Aviatrix launch request", e);
        }
    }

    @Override
    public boolean isSuccess(GameLaunchResponse response) {
        if (response == null || response.getUrl() == null || response.getUrl().isBlank()) {
            return false;
        }
        try {
            URI uri = URI.create(response.getUrl());
            return uri.getHost() != null
                    && ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public void onSuccess(GameLaunchContext context, GameLaunchResponse response) {
        context.setGameUrl(response.getUrl());
    }
}
