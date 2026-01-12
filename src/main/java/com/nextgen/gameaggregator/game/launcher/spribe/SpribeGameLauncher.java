package com.nextgen.gameaggregator.game.launcher.spribe;

import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.engine.game.url.QueryStringUrlGameLauncher;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import com.nextgen.gameaggregator.vendor.spribe.config.SpribeConfig;
import com.nextgen.gameaggregator.vendor.spribe.constant.Credentials;
import org.springframework.stereotype.Service;

@Service
public class SpribeGameLauncher extends QueryStringUrlGameLauncher<GameLaunchRequest> {
    private static final String OPERATOR = "operator";

    public SpribeGameLauncher(VendorCredentialUtils credentialUtils) {
        super(credentialUtils, SpribeConfig.CLASS_NAME);
    }

    @Override
    public String getBaseUrl(GameLaunchContext context) {
        VendorCredentialAccessor accessor = credentials(context.getVendorCredentials());
        return "https://" + accessor.getValue(Credentials.GAME_URL);
    }

    @Override
    public String getPath(GameLaunchContext context) {
        return "/" + context.getVendorGameCode();
    }

    @Override
    public GameLaunchRequest buildRequestBody(GameLaunchContext context) {
        VendorCredentialAccessor accessor = credentials(context.getVendorCredentials());
        String operator = accessor.getOrDefault(OPERATOR, "");

        return GameLaunchRequest.builder()
                .user(context.getVendorPlayerUsername())
                .token(context.getToken())
                .lang(context.getVendorLanguageCode())
                .currency(context.getVendorCurrencyCode())
                .operator(operator)
                .build();
    }
}
