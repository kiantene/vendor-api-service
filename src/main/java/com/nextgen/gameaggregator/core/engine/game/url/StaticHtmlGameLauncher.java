package com.nextgen.gameaggregator.core.engine.game.url;

import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import org.springframework.http.MediaType;

public abstract class StaticHtmlGameLauncher<T> extends AbstractGameLaunchHandler<T, String> {

    protected StaticHtmlGameLauncher(VendorCredentialUtils credentialUtils, String vendorClassName) {

        super(credentialUtils, vendorClassName, String.class);
    }

    @Override
    public GameLaunchMode getLaunchMode() {
        return GameLaunchMode.STATIC_HTML;
    }

    @Override
    public MediaType getContentType() {
        return MediaType.TEXT_HTML;
    }

    @Override
    public String getBaseUrl(GameLaunchContext context) {
        return null;
    }

    @Override
    public String getPath(GameLaunchContext context) {
        return null;
    }
}
