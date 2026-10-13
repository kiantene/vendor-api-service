package com.nextgen.gameaggregator.game.launcher.megafair;

import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchHandler;
import com.nextgen.gameaggregator.core.engine.game.url.QueryStringUrlGameLauncher;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import com.nextgen.gameaggregator.vendor.megafair.config.MegafairConfig;
import com.nextgen.gameaggregator.vendor.megafair.constant.Credentials;
import com.nextgen.gameaggregator.vendor.megafair.constant.EndPoints;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

@Service(EndPoints.CLASS_NAME + GameLaunchHandler.NAME)
public class MegafairGameLauncher extends QueryStringUrlGameLauncher<GameLaunchRequest> {

    protected MegafairGameLauncher(VendorCredentialUtils credentialUtils) {
        super(credentialUtils, MegafairConfig.CLASS_NAME);
    }

    @Override
    public String getBaseUrl(GameLaunchContext context) {
        VendorCredentialAccessor accessor = credentials(context.getVendorCredentials());
        return accessor.getValue(Credentials.BASE_URL);
    }

    @Override
    public String getPath(GameLaunchContext context) {
        return EndPoints.LAUNCH_GAME;
    }

    @Override
    public GameLaunchRequest buildRequestBody(GameLaunchContext context) {
        VendorCredentialAccessor accessor = credentials(context.getVendorCredentials());
        String bankId = accessor.getValue(Credentials.BANK_ID);

        return GameLaunchRequest.builder()
                .token(context.getToken())
                .bankId(bankId)
                .gameId(context.getVendorGameCode())
                .mode("real")
                .build();
    }

    @Override
    public HttpMethod getMethod() {
        return HttpMethod.GET;
    }

}
