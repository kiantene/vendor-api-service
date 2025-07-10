package com.nextgen.gameaggregator.core.engine.game.url;

import com.nextgen.gameaggregator.core.common.WebClientApiCaller;
import com.nextgen.gameaggregator.core.exception.InternalConfigurationException;
import com.nextgen.gameaggregator.service.S3Service;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

@Service
public class GameLaunchService {
    private final S3Service s3Service;

    public GameLaunchService(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    public void processLaunchRequest(GameLaunchHandler<Object, Object> launchHandler, GameLaunchContext context) {
        MediaType contentType = launchHandler.getContentType();

        Object response = null;
        if (shouldCallWebClient(contentType)) {
            response = callWebClient(launchHandler, context);
        }

        launchHandler.onSuccess(context, response);
    }

    private boolean shouldCallWebClient(MediaType contentType) {
        return MediaType.APPLICATION_JSON.equals(contentType) ||
                MediaType.APPLICATION_FORM_URLENCODED.equals(contentType);
    }

    private Object callWebClient(GameLaunchHandler<Object, Object> launchHandler, GameLaunchContext context) {
        String vendorClassName = context.getVendorClassName();
        Object gameLaunchRequest = launchHandler.onPrepareRequestBody(context);

        String baseUrl = launchHandler.getBaseUrl(context);
        if (baseUrl == null) throw new InternalConfigurationException(vendorClassName + " Game Launch baseUrl cannot be found.");

        WebClientApiCaller webClientApiCaller = new WebClientApiCaller(
                launchHandler.getPath(),
                launchHandler.getContentType()
        );

        return webClientApiCaller.post(
                baseUrl,
                launchHandler.getHeaders(context, gameLaunchRequest),
                gameLaunchRequest,
                launchHandler.getResponseType()
        );
    }

    private String callS3(GameLaunchHandler<Object, Object> launchHandler, GameLaunchContext context) {
        String vendorClassName = context.getVendorClassName();
        Object gameLaunchRequest = launchHandler.onPrepareRequestBody(context);


    }
}
