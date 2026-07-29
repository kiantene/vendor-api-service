package com.nextgen.gameaggregator.game.launcher.wazdan;

import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchHandler;
import com.nextgen.gameaggregator.core.engine.game.url.QueryStringUrlGameLauncher;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import com.nextgen.gameaggregator.vendor.Vendors;
import com.nextgen.gameaggregator.vendor.wazdan.config.WazdanConfig;
import com.nextgen.gameaggregator.vendor.wazdan.constant.Credentials;
import com.nextgen.gameaggregator.vendor.wazdan.constant.EndPoints;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

@Service(EndPoints.CLASS_NAME + GameLaunchHandler.NAME)
public class WazdanGameLauncher extends QueryStringUrlGameLauncher<GameLaunchRequest> {

    protected WazdanGameLauncher(VendorCredentialUtils credentialUtils) {
        super(credentialUtils, WazdanConfig.CLASS_NAME);
    }

    @Override
    public String getBaseUrl(GameLaunchContext context) {
        VendorCredentialAccessor accessor = credentials(context.getVendorCredentials());
        return accessor.getValue(Credentials.GAME_LAUNCH);
    }

    @Override
    public String getPath(GameLaunchContext context) {
        return "";
    }

    @Override
    public GameLaunchRequest buildRequestBody(GameLaunchContext context) {
        VendorCredentialAccessor accessor = credentials(context.getVendorCredentials());
        String operatorId = accessor.getValue(Credentials.OPERATOR_ID);
        String license = accessor.getValue(Credentials.LICENSE);

        return GameLaunchRequest.builder()
                .operator(operatorId)
                .mode("real")
                .game(context.getVendorGameCode())
                .token(context.getToken())
                .license(license)
                .lang(context.getVendorLanguageCode())
                .platform(context.getVendorPlatformCode())
                .build();
    }

    @Override
    public HttpMethod getMethod() {
        return HttpMethod.GET;
    }

}
