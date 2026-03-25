package com.nextgen.gameaggregator.game.launcher.cosmoplay;

import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.engine.game.url.QueryStringUrlGameLauncher;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import com.nextgen.gameaggregator.vendor.cosmoplay.config.CosmoPlayVendorConfig;
import com.nextgen.gameaggregator.vendor.cosmoplay.constant.Credentials;
import org.springframework.stereotype.Service;

@Service(CosmoPlayVendorConfig.GAME_LAUNCHER_SERVICE_NAME)
public class CosmoPlayGameLauncher extends QueryStringUrlGameLauncher<GameLaunchRequest> {

    protected CosmoPlayGameLauncher(VendorCredentialUtils credentialUtils) {
        super(credentialUtils, CosmoPlayVendorConfig.CLASS_NAME);
    }

    @Override
    public String getBaseUrl(GameLaunchContext context) {
        VendorCredentialAccessor accessor = this.credentials(
                context.getVendorCredentials()
        );

        return accessor.getValue(Credentials.H5_DOMAIN);
    }

    @Override
    public String getPath(GameLaunchContext gameLaunchContext) {
        return "";
    }

    @Override
    public GameLaunchRequest buildRequestBody(GameLaunchContext context) {
        VendorCredentialAccessor accessor = this.credentials(
                context.getVendorCredentials()
        );

        String playerID = context.getVendorPlayerUsername();
        String partnerCode = accessor.getValue(Credentials.PARTNER_CODE);

        return GameLaunchRequest.builder()
                .gid(context.getVendorGameCode())  // Changed from .gameID()
                .pid(partnerCode.trim() + "-" + playerID.trim())  // Changed from .playerID()
                .atk(context.getToken())  // Changed from .authToken()
                .language(CosmoPlayVendorConfig.language(context.getVendorLanguageCode()))
                .url(context.getLobbyUrl())// Changed from .hostURL()
                .build();
    }
}
