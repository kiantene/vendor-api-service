package com.nextgen.gameaggregator.game.launcher.aviatorstudio;

import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.engine.game.url.QueryStringUrlGameLauncher;
import com.nextgen.gameaggregator.core.exception.GameLaunchException;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import com.nextgen.gameaggregator.vendor.aviatorstudio.config.AviatorStudioConfig;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.Credentials;
import com.nextgen.gameaggregator.vendor.aviatorstudio.util.EncryptUtil;
import com.nextgen.gameaggregator.vendor.aviatorstudio.util.JwtUtil;
import org.springframework.stereotype.Service;

@Service
public class AviatorStudioGameLauncher extends QueryStringUrlGameLauncher<GameLaunchRequest> {

    protected AviatorStudioGameLauncher(VendorCredentialUtils credentialUtils) {
        super(credentialUtils, AviatorStudioConfig.CLASS_NAME);
    }

    @Override
    public String getBaseUrl(GameLaunchContext context) {
        VendorCredentialAccessor accessor = credentials(context.getVendorCredentials());
        return accessor.getValue(Credentials.API_URL);
    }

    @Override
    public String getPath(GameLaunchContext gameLaunchContext) {
        return "";
    }

    @Override
    public GameLaunchRequest buildRequestBody(GameLaunchContext context) {
        VendorCredentialAccessor accessor = credentials(context.getVendorCredentials());
        String publicKey = accessor.getValue(Credentials.PUBLIC_KEY);
        String jwtSecret = accessor.getValue(Credentials.JWT_SECRET);
        String userid = context.getVendorPlayerUsername();

        String jwt = JwtUtil.generateJwt(userid, context.getToken(), jwtSecret);
        LogContext.putField("jwt", jwt);
        try {
            String encrypted = EncryptUtil.encrypt(jwt, publicKey);
            return GameLaunchRequest.builder()
                    .token(encrypted)
                    .providerId(accessor.getValue(Credentials.PROVIDER_ID))
                    .currency(context.getVendorCurrencyCode())
                    .language(context.getVendorLanguageCode())
                    .gameId(context.getVendorGameCode())
                    .build();
        } catch (Exception ex) {
            throw new GameLaunchException(ex.getMessage(), ex);
        }
    }
}
