package com.nextgen.gameaggregator.game.launcher.egtdigital;

import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchHandler;
import com.nextgen.gameaggregator.core.engine.game.url.QueryStringUrlGameLauncher;
import com.nextgen.gameaggregator.core.exception.GameLaunchException;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import com.nextgen.gameaggregator.vendor.egtdigital.constant.Credentials;
import com.nextgen.gameaggregator.vendor.egtdigital.constant.Demo;
import com.nextgen.gameaggregator.vendor.egtdigital.constant.EndPoints;
import org.springframework.stereotype.Service;

@Service(EndPoints.CLASS_NAME + GameLaunchHandler.NAME)
public class EgtDigitalGameLauncher extends QueryStringUrlGameLauncher<GameLaunchRequest> {

    protected EgtDigitalGameLauncher(VendorCredentialUtils credentialUtils) {
        super(credentialUtils, EndPoints.CLASS_NAME);
    }

    @Override
    public String getBaseUrl(GameLaunchContext context) {
        VendorCredentialAccessor accessor = credentials(context.getVendorCredentials());
        return accessor.getValue(Credentials.HOST);
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
                    .sessionToken(context.getToken())
                    .casinoId(accessor.getValue(Credentials.CASINO_ID))
                    .playerId(context.getVendorPlayerUsername())
                    .gameKey(context.getVendorGameCode())
                    .currencyCode(context.getVendorCurrencyCode())
                    .closeUrl(context.getLobbyUrl())
                    .demo(Demo.INACTIVE)
                    .build();
        } catch (Exception ex) {
            throw new GameLaunchException(ex.getMessage(), ex);
        }
    }
}
