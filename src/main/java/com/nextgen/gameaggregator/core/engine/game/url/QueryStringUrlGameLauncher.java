package com.nextgen.gameaggregator.core.engine.game.url;

import com.nextgen.core.security.signature.SigningStrategyType;
import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import org.springframework.http.MediaType;

public abstract class QueryStringUrlGameLauncher<T> extends AbstractGameLaunchHandler<T, String> {

    protected QueryStringUrlGameLauncher(VendorCredentialUtils credentialUtils,
                                         String vendorClassName) {

        super(credentialUtils, vendorClassName, String.class);
    }

    protected QueryStringUrlGameLauncher(VendorCredentialUtils credentialUtils,
                                         String vendorClassName,
                                         SigningStrategyType strategyType) {

        super(credentialUtils, vendorClassName, String.class, strategyType);
    }

    @Override
    public GameLaunchMode getLaunchMode() {
        return GameLaunchMode.QUERY_STRING_URL;
    }

    @Override
    public MediaType getContentType() {
        return MediaType.TEXT_PLAIN;
    }

    @Override
    public boolean isSuccess(String response) {
        return true;
    }

    @Override
    public void onSuccess(GameLaunchContext context, String response) {
        context.setGameUrl(response);
    }
}
