package com.nextgen.gameaggregator.core.engine.game.url;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;

public interface GameLaunchHandler<T> {
    String getVendorClassName();
    ParameterizedTypeReference<T> getResponseType();
    Object onPrepareRequestBody(GameLaunchContext context);
    void onSuccess(GameLaunchContext context, T response);
    default void onError(GameLaunchContext context, Throwable error) {
        // Optional override
    }
    String getPath();
    String getBaseUrl(GameLaunchContext context);
    default MediaType getContentType() {
        return MediaType.APPLICATION_FORM_URLENCODED;
    }
}
