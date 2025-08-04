package com.nextgen.gameaggregator.game.launcher.inout;

import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchHandler;
import com.nextgen.gameaggregator.core.engine.game.url.QueryStringUrlGameLauncher;
import com.nextgen.gameaggregator.core.signature.SigningStrategyType;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import com.nextgen.gameaggregator.entity.ga.VendorLineCredential;
import com.nextgen.gameaggregator.vendor.inout.constant.Credentials;
import com.nextgen.gameaggregator.vendor.inout.constant.EndPoints;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service(EndPoints.CLASS_NAME + GameLaunchHandler.NAME)
public class InoutGameLauncher extends QueryStringUrlGameLauncher<GameLaunchRequest> {

    protected InoutGameLauncher(VendorCredentialUtils credentialUtils) {
        super(credentialUtils, EndPoints.CLASS_NAME, SigningStrategyType.HMAC_SHA256);
    }

    @Override
    public String getBaseUrl(GameLaunchContext context) {
        VendorCredentialAccessor credentialAccessor = credentials(context.getVendorCredentials());
        VendorLineCredential urlSchemeCredential = credentialAccessor.get(Credentials.API_FRAME_URL);
        return urlSchemeCredential.getValue();
    }

    @Override
    public String getPath(GameLaunchContext gameLaunchContext) {
        return "";
    }

    @Override
    public GameLaunchRequest buildRequestBody(GameLaunchContext context) {
        VendorCredentialAccessor accessor = credentials(context.getVendorCredentials());
        String secretKey = accessor.getValue(Credentials.SECRET_KEY);
        String operatorId = accessor.getValue(Credentials.OPERATOR_ID);
        String subId = accessor.getValue(Credentials.NAME);

        return GameLaunchRequest.builder()
                .gameMode(context.getVendorGameCode())
                .aggregatorId(UUID.fromString(operatorId))
                .currency(context.getVendorCurrencyCode())
                .authToken(context.getToken())
                .lang(context.getVendorLanguageCode())
                .themeId(UUID.fromString(operatorId))
                .lobbyUrl(context.getLobbyUrl())
                .subId(subId)
                .isDemoPlay(false)
                .token(sign(operatorId + ":" + subId, secretKey))
                .build();
    }
}
