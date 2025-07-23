package com.nextgen.gameaggregator.core.engine.game.url;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;

public abstract class QueryStringUrlGameLauncher<T> extends AbstractGameLaunchHandler<T, String> {
    private static final ParameterizedTypeReference<String> RESPONSE_TYPE = new ParameterizedTypeReference<>() {};

    @Override
    public GameLaunchMode getLaunchMode() {
        return GameLaunchMode.QUERY_STRING_URL;
    }

    @Override
    public ParameterizedTypeReference<String> getResponseType() {
        return RESPONSE_TYPE;
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
