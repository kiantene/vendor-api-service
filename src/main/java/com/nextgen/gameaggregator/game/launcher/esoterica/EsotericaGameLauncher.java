package com.nextgen.gameaggregator.game.launcher.esoterica;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.core.security.signature.SigningStrategyType;
import com.nextgen.gameaggregator.core.engine.game.url.AbstractGameLaunchHandler;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchHandler;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import com.nextgen.gameaggregator.entity.ga.VendorLineCredential;
import com.nextgen.gameaggregator.vendor.esoterica.constant.StringConstants;
import com.nextgen.gameaggregator.vendor.esoterica.constant.Credentials;
import com.nextgen.gameaggregator.vendor.esoterica.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.esoterica.util.VendorUtil;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service(EndPoints.CLASS_NAME + GameLaunchHandler.NAME)
public class EsotericaGameLauncher extends AbstractGameLaunchHandler<GameLaunchRequest, GameLaunchResponse> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    protected EsotericaGameLauncher(VendorCredentialUtils credentialUtils) {
        super(credentialUtils, EndPoints.CLASS_NAME, GameLaunchResponse.class, SigningStrategyType.SHA256_HEX);
    }

    @Override
    public String getBaseUrl(GameLaunchContext gameLaunchContext) {
        VendorCredentialAccessor credentialAccessor = credentials(gameLaunchContext.getVendorCredentials());
        VendorLineCredential urlSchemeCredential = credentialAccessor.get(Credentials.API_URL);
        return urlSchemeCredential.getValue();
    }

    @Override
    public String getPath(GameLaunchContext gameLaunchContext) {
        return EndPoints.LAUNCH_PATH;
    }

    @Override
    public GameLaunchRequest buildRequestBody(GameLaunchContext gameLaunchContext) {

        VendorCredentialAccessor accessor = credentials(gameLaunchContext.getVendorCredentials());
        String secretLogin = accessor.getValue(Credentials.SECRET_LOGIN);
        String secretKey = accessor.getValue(Credentials.SECRET_KEY);

        GameLaunchRequest request = GameLaunchRequest.builder()
                .secretLogin(secretLogin)
                .gameName(gameLaunchContext.getVendorGameCode())
                .externalPlayerId(gameLaunchContext.getVendorPlayerUsername())
                .playMode(StringConstants.REAL)
                .token(gameLaunchContext.getToken())
                .build();

        Map<String, Object> params = OBJECT_MAPPER.convertValue(request, new TypeReference<>() {});

        String concatenatedString = VendorUtil.prepareRequest(params, secretKey, null);
        String hashedString = sign(concatenatedString, "").toUpperCase();
        request.setHash(hashedString);

        return request;
    }

    @Override
    public void onSuccess(GameLaunchContext context, GameLaunchResponse response) {
        context.setGameUrl(response.getData().getLaunchUrl());
    }

    @Override
    public boolean isSuccess(GameLaunchResponse gameLaunchResponse) {
        return gameLaunchResponse != null
                && gameLaunchResponse.getData() != null
                && gameLaunchResponse.getData().getLaunchUrl() != null;
    }

    @Override
    public MediaType getContentType() {
        return MediaType.APPLICATION_JSON;
    }
}
