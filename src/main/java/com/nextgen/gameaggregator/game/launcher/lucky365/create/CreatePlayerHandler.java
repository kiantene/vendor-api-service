package com.nextgen.gameaggregator.game.launcher.lucky365.create;

import com.nextgen.core.security.signature.SigningStrategyType;
import com.nextgen.gameaggregator.core.engine.game.url.AbstractGameLaunchHandler;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import com.nextgen.gameaggregator.game.launcher.lucky365.GameLaunchRequest;
import com.nextgen.gameaggregator.vendor.lucky365.constant.*;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class CreatePlayerHandler extends AbstractGameLaunchHandler<CreatePlayerRequest, CreatePlayerResponse> {
    public CreatePlayerHandler(VendorCredentialUtils credentialUtils) {
        super(credentialUtils, EndPoints.CLASS_NAME + "CreatePlayerHandler", CreatePlayerResponse.class, SigningStrategyType.MD5);
    }

    @Override
    public void onSuccess(GameLaunchContext context, CreatePlayerResponse response) {
        // do nothing
    }

    @Override
    public String getBaseUrl(GameLaunchContext context) {
        VendorCredentialAccessor credentialAccessor = credentials(context.getVendorCredentials());
        return credentialAccessor.getValue(Credentials.API_URL);
    }

    @Override
    public String getPath(GameLaunchContext context) {
        return EndPoints.CREATE_PLAYER;
    }

    @Override
    public CreatePlayerRequest buildRequestBody(GameLaunchContext context) {
        VendorCredentialAccessor accessor = credentials(context.getVendorCredentials());
        String sn = accessor.getValue(Credentials.SERIAL_NUM);
        String secretKey = accessor.getValue(Credentials.SECRET_KEY);
        String encryptString = context.getToken() + Method.CREATE_PLAYER + sn + context.getVendorPlayerUsername() + secretKey;
        String signature=sign(encryptString,"");

        return CreatePlayerRequest.builder()
                .sn(sn)
                .id(context.getToken())
                .method(Method.CREATE_PLAYER)
                // ONEAPI-244: use the vendor username (hyphen-free) instead of the agent username
                // for PlayerName too - Lucky365 rejects CreatePlayer when PlayerName has a hyphen.
                .playerName(context.getVendorPlayerUsername())
                .playerCode(context.getVendorPlayerUsername())
                .signature(signature)
                .build();
    }

    @Override
    public boolean isSuccess(CreatePlayerResponse response) {
        return response.isSuccess();
    }

    @Override
    public MediaType getContentType() {
        return MediaType.APPLICATION_JSON;
    }

}
