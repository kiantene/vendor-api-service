package com.nextgen.gameaggregator.game.launcher.topbet;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.core.engine.game.url.AbstractGameLaunchHandler;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchHandler;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import com.nextgen.gameaggregator.vendor.topbet.constant.Credentials;
import com.nextgen.gameaggregator.vendor.topbet.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.topbet.constant.Method;
import com.nextgen.gameaggregator.vendor.topbet.service.VendorUtil;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service(EndPoints.CLASS_NAME + GameLaunchHandler.NAME)
public class TopbetGameLauncher extends AbstractGameLaunchHandler<GameLaunchRequest, GameLaunchResponse> {
    private final MemberCreateService memberCreateService;

    public TopbetGameLauncher(VendorCredentialUtils credentialUtils,
                              MemberCreateService memberCreateService) {

        super(credentialUtils, EndPoints.CLASS_NAME, GameLaunchResponse.class);
        this.memberCreateService = memberCreateService;
    }

    @Override
    public String getPath(GameLaunchContext context) {
        return EndPoints.LOGIN_USER;
    }

    @Override
    public String getBaseUrl(GameLaunchContext context) {
        VendorCredentialAccessor credentialAccessor = credentials(context.getVendorCredentials());
        return credentialAccessor.getValue(Credentials.API_URL);
    }

    @Override
    public AbstractGameLaunchHandler<GameLaunchRequest, GameLaunchResponse> prepareLaunchRequest(GameLaunchContext context) {
        memberCreateService.process(context);
        return super.prepareLaunchRequest(context);
    }

    @Override
    public GameLaunchRequest buildRequestBody(GameLaunchContext context) {
        VendorCredentialAccessor accessor = credentials(context.getVendorCredentials());
        ObjectMapper objectMapper = new ObjectMapper();
        GameLaunchRequest request = GameLaunchRequest.builder()
                .pid(accessor.getValue(Credentials.MERCHANT_ID))
                .ver(accessor.getValue(Credentials.VERSION))
                .method(Method.LOGIN.value)
                .username(context.getVendorPlayerUsername())
                .app_id(Integer.valueOf(context.getVendorGameCode()))
                .ip(context.getIpAddress())
                .lang(context.getVendorLanguageCode())
                .build();

        // Convert DTO to Map
        Map<String, Object> paramMap = objectMapper.convertValue(request, new TypeReference<>() {
        });
        // sort params by sequence "abc" and generate signature
        String signature = VendorUtil.getSignature(paramMap, accessor.getValue(Credentials.API_KEY));

        return GameLaunchRequest.builder()
                .pid(accessor.getValue(Credentials.MERCHANT_ID))
                .ver(accessor.getValue(Credentials.VERSION))
                .method(Method.LOGIN.value)
                .username(context.getVendorPlayerUsername())
                .app_id(Integer.valueOf(context.getVendorGameCode()))
                .ip(context.getIpAddress())
                .lang(context.getVendorLanguageCode())
                .sign(signature)
                .build();
    }

    @Override
    public boolean isSuccess(GameLaunchResponse response) {
        return response.getCode() == 0 && response.getUrl() != null;
    }

    @Override
    public void onSuccess(GameLaunchContext context, GameLaunchResponse response) {
        context.setGameUrl(response.getUrl());
    }

    @Override
    public MediaType getContentType() {
        return MediaType.APPLICATION_JSON;
    }
}
