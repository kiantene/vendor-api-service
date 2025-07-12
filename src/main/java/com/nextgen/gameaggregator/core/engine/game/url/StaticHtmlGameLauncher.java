package com.nextgen.gameaggregator.core.engine.game.url;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;

public abstract class StaticHtmlGameLauncher<T> implements GameLaunchHandler<T, String> {
    private static final ParameterizedTypeReference<String> RESPONSE_TYPE = new ParameterizedTypeReference<>() {};

    @Override
    public GameLaunchMode getLaunchMode() {
        return GameLaunchMode.STATIC_HTML;
    }

    @Override
    public ParameterizedTypeReference<String> getResponseType() {
        return RESPONSE_TYPE;
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
    public String getPath() {
        return null;
    }
}
