package com.nextgen.gameaggregator.core.engine.game.url;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;

import java.util.Collections;
import java.util.Map;

public interface GameLaunchHandler<R, T> {
    String getVendorClassName();
    default MediaType getContentType() {
        return MediaType.APPLICATION_FORM_URLENCODED;
    }
    String getBaseUrl(GameLaunchContext context);
    String getPath();
    ParameterizedTypeReference<T> getResponseType();
    R onPrepareRequestBody(GameLaunchContext context);
    default Map<String, String> getHeaders(GameLaunchContext context, R requestObject) {
        return Collections.emptyMap();
    }
    void onSuccess(GameLaunchContext context, T response);
    default void onError(GameLaunchContext context, Throwable error) {
        // Optional override
    }
}
