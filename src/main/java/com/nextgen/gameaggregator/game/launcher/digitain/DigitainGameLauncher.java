package com.nextgen.gameaggregator.game.launcher.digitain;

import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchHandler;
import com.nextgen.gameaggregator.core.engine.game.url.QueryStringUrlGameLauncher;
import com.nextgen.gameaggregator.core.exception.GameLaunchException;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import com.nextgen.gameaggregator.vendor.digitain.constant.Credentials;
import com.nextgen.gameaggregator.vendor.digitain.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.digitain.constant.Mode;
import org.springframework.stereotype.Service;


@Service(EndPoints.CLASS_NAME + GameLaunchHandler.NAME)
public class DigitainGameLauncher extends QueryStringUrlGameLauncher<GameLaunchRequest> {

    protected DigitainGameLauncher(VendorCredentialUtils credentialUtils) {
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
        try {
            return GameLaunchRequest.builder()
                    .dvt(Integer.valueOf(context.getVendorPlatformCode()))
                    .gid(context.getVendorGameCode())
                    .hmu(context.getLobbyUrl())
                    .lng(context.getVendorLanguageCode())
                    .oid(accessor.getValue(Credentials.OPERATOR_ID))
                    .plm(Mode.REAL)
                    .tkn(context.getToken())
                    .build();
        } catch (Exception ex) {
            throw new GameLaunchException(ex.getMessage(), ex);
        }
    }
}
