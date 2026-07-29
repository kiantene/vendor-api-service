package com.nextgen.gameaggregator.game.launcher.groove;

import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchHandler;
import com.nextgen.gameaggregator.core.engine.game.url.QueryStringUrlGameLauncher;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import com.nextgen.gameaggregator.util.GeoIpUtil;
import com.nextgen.gameaggregator.vendor.groove.constant.Credentials;
import com.nextgen.gameaggregator.vendor.groove.constant.EndPoints;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

@Service(EndPoints.CLASS_NAME + GameLaunchHandler.NAME)
public class GrooveGameLauncher extends QueryStringUrlGameLauncher<GameLaunchRequest> {

    protected GrooveGameLauncher(VendorCredentialUtils credentialUtils) {
        super(credentialUtils, EndPoints.CLASS_NAME);
    }

    @Override
    public String getBaseUrl(GameLaunchContext context) {
        VendorCredentialAccessor accessor = credentials(context.getVendorCredentials());
        return accessor.getValue(Credentials.API_URL);
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
        String sessionId = operatorId + "_" + context.getToken();
        String countryCode = GeoIpUtil.getCountryCode(context.getIpAddress());

        return GameLaunchRequest.builder()
                .accountid(context.getVendorPlayerUsername())
                .country(countryCode)
                .nogsgameid(Integer.valueOf(context.getVendorGameCode()))
                .nogslang(context.getVendorLanguageCode())
                .nogsmode("real")
                .nogsoperatorid(operatorId)
                .nogscurrency(context.getVendorCurrencyCode())
                .sessionid(sessionId)
                .homeurl(context.getLobbyUrl())
                .license(license)
                .is_test_account(false)
                .device_type(context.getVendorPlatformCode())
                .build();
    }

    @Override
    public HttpMethod getMethod() {
        return HttpMethod.GET;
    }

}
