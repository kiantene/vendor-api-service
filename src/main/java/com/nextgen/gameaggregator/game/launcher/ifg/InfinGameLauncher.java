package com.nextgen.gameaggregator.game.launcher.ifg;

import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchHandler;
import com.nextgen.gameaggregator.core.engine.game.url.QueryStringUrlGameLauncher;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import com.nextgen.gameaggregator.entity.ga.VendorLineCredential;
import com.nextgen.gameaggregator.vendor.ifg.constant.Credentials;
import com.nextgen.gameaggregator.vendor.ifg.constant.EndPoints;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service(EndPoints.CLASS_NAME + GameLaunchHandler.NAME)
public class InfinGameLauncher extends QueryStringUrlGameLauncher<LaunchRequestPayload> {
    private static final String GAME_TYPE_DEMO_FALSE = "false";
    private Integer version = 1;
    private final Map<Integer, String> vendorPlatformCodes = Map.of(
            1, "mob",
            2, "desktop"
    );

    public InfinGameLauncher(VendorCredentialUtils credentialUtils) {

        super(credentialUtils, EndPoints.CLASS_NAME);
    }

    @Override
    public String getBaseUrl(GameLaunchContext context) {
        VendorCredentialAccessor credentialAccessor = credentials(context.getVendorCredentials());
        VendorLineCredential urlSchemeCredential = credentialAccessor.get(Credentials.game_url);
        this.version = urlSchemeCredential.getVersion();
        return urlSchemeCredential.getValue();
    }

    @Override
    public String getPath(GameLaunchContext context) {
        return "";
    }

    @Override
    public LaunchRequestPayload buildRequestBody(GameLaunchContext context) {
        VendorCredentialAccessor credentialAccessor = credentials(context.getVendorCredentials());
        String vendorGameCode = context.getVendorGameCode();
        String partner = credentialAccessor.getValue(Credentials.partner);
        String token = context.getToken();
        String platform = vendorPlatformCodes.getOrDefault(context.getPlatformId(), "desktop");
        String lang = context.getVendorLanguageCode();

        if (this.version > 1) {
            return GameLaunchRequestV2.builder()
                    .project(partner)
                    .game(vendorGameCode)
                    .platform(platform)
                    .lang(lang)
                    .demo(GAME_TYPE_DEMO_FALSE)
                    .auth(token)
                    .build();
        }

        return GameLaunchRequestV1.builder()
                .partner(partner)
                .gameName(vendorGameCode)
                .platform(platform)
                .lang(lang)
                .demo(GAME_TYPE_DEMO_FALSE)
                .key(token)
                .build();
    }
}
