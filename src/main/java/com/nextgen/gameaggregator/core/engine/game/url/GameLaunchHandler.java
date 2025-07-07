package com.nextgen.gameaggregator.core.engine.game.url;

import org.springframework.http.MediaType;

public interface GameLaunchHandler {
    String getVendorClassName();
    Object onPrepareRequestBody(GameLaunchContext context);
    void onSuccess();

    default MediaType getContentType() {
        return MediaType.APPLICATION_FORM_URLENCODED;
    }
}
