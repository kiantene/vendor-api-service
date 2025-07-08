package com.nextgen.gameaggregator.core.engine.game.url;

import com.nextgen.gameaggregator.core.common.WebClientApiCaller;
import com.nextgen.gameaggregator.core.exception.InternalConfigurationException;
import org.springframework.stereotype.Service;

@Service
public class GameLaunchService {
    public void process(GameLaunchHandler<Object, Object> launchHandler, GameLaunchContext context) {
        String vendorClassName = context.getVendorClassName();
        Object gameLaunchRequest = launchHandler.onPrepareRequestBody(context);
        WebClientApiCaller webClientApiCaller = new WebClientApiCaller(
                launchHandler.getPath(),
                launchHandler.getContentType()
        );

        String baseUrl = launchHandler.getBaseUrl(context);
        if (baseUrl == null) throw new InternalConfigurationException(vendorClassName + " Game Launch baseUrl cannot be found.");

        Object response = webClientApiCaller.post(
                baseUrl,
                launchHandler.getHeaders(context, gameLaunchRequest),
                gameLaunchRequest,
                launchHandler.getResponseType()
        );
        launchHandler.onSuccess(context, response);
    }
}
