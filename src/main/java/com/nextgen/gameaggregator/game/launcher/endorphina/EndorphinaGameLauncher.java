package com.nextgen.gameaggregator.game.launcher.endorphina;

import com.nextgen.core.security.signature.SigningStrategyType;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.engine.game.url.QueryStringUrlGameLauncher;
import com.nextgen.gameaggregator.core.exception.GameLaunchException;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import com.nextgen.gameaggregator.game.launcher.endorphina.util.VendorUtil;
import com.nextgen.gameaggregator.vendor.endorphina.config.EndorphinaConfig;
import com.nextgen.gameaggregator.vendor.endorphina.constant.Credentials;
import com.nextgen.gameaggregator.vendor.endorphina.constant.EndPoints;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

@Service
public class EndorphinaGameLauncher extends QueryStringUrlGameLauncher<GameLaunchRequest> {

    private static final String DEFAULT_LANGUAGE = "en";

    protected EndorphinaGameLauncher(VendorCredentialUtils credentialUtils) {
        super(credentialUtils, EndorphinaConfig.CLASS_NAME, SigningStrategyType.SHA1_HEX);
    }

    @Override
    public String getBaseUrl(GameLaunchContext context) {
        VendorCredentialAccessor accessor = credentials(context.getVendorCredentials());
        return accessor.getValue(Credentials.ENDO_URL);
    }

    @Override
    public String getPath(GameLaunchContext gameLaunchContext) {
        return EndPoints.LAUNCH_PATH;
    }

    @Override
    public GameLaunchRequest buildRequestBody(GameLaunchContext context) {
        VendorCredentialAccessor accessor = credentials(context.getVendorCredentials());
        String nodeId = accessor.getValue(Credentials.NODE_ID);
        String salt = accessor.getValue(Credentials.SALT);

        String lang = Objects.toString(context.getVendorLanguageCode(), DEFAULT_LANGUAGE);
        if (lang.isBlank()) {
            lang = DEFAULT_LANGUAGE;
        }

        context.setVendorToken(VendorUtil.removeDash(context.getToken()));

        // Sort params by sequence "abc"
        Map<String, String> sortedParams = VendorUtil.buildSortedParams(context.getLobbyUrl(), lang, context.getVendorToken(), nodeId);
        String queryParams = VendorUtil.getSignature(sortedParams, salt);
        String finalSign = sign(queryParams, "");

        try {
            return GameLaunchRequest.builder()
                    .exit(context.getLobbyUrl())
                    .lang(lang)
                    .nodeId(nodeId)
                    .token(context.getVendorToken())
                    .sign(finalSign)
                    .build();
        } catch (Exception ex) {
            throw new GameLaunchException(ex.getMessage(), ex);
        }
    }
}