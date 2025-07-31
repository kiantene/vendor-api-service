package com.nextgen.gameaggregator.core.engine.game.url;

import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;

public abstract class QueryStringUrlGameLauncher<T> extends AbstractGameLaunchHandler<T, String> {

    protected QueryStringUrlGameLauncher(VendorCredentialUtils credentialUtils,
                                         String vendorClassName) {

        super(credentialUtils, vendorClassName, String.class);
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
    public void onSuccess(GameLaunchContext context, String response) {
        context.setGameUrl(response);
    }
}
