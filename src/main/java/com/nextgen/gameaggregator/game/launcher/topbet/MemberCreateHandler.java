package com.nextgen.gameaggregator.game.launcher.topbet;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.core.engine.game.url.AbstractGameLaunchHandler;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.core.security.signature.SigningStrategyType;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import com.nextgen.gameaggregator.vendor.topbet.constant.Credentials;
import com.nextgen.gameaggregator.vendor.topbet.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.topbet.constant.Method;
import com.nextgen.gameaggregator.vendor.topbet.service.VendorUtil;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MemberCreateHandler extends AbstractGameLaunchHandler<MemberCreateRequest, MemberCreateResponse> {
    public MemberCreateHandler(VendorCredentialUtils credentialUtils) {
        super(credentialUtils, EndPoints.CLASS_NAME + "MemberCreateHandler", MemberCreateResponse.class, SigningStrategyType.MD5);
    }

    @Override
    public void onSuccess(GameLaunchContext context, MemberCreateResponse response) {
        // do nothing
    }

    @Override
    public String getBaseUrl(GameLaunchContext context) {
        VendorCredentialAccessor credentialAccessor = credentials(context.getVendorCredentials());
        return credentialAccessor.getValue(Credentials.API_URL);
    }

    @Override
    public String getPath(GameLaunchContext context) {
        return EndPoints.CREATE_USER;
    }

    @Override
    public MemberCreateRequest buildRequestBody(GameLaunchContext context) {
        VendorCredentialAccessor accessor = credentials(context.getVendorCredentials());
        ObjectMapper objectMapper = new ObjectMapper();
        MemberCreateRequest request = MemberCreateRequest.builder()
                .pid(accessor.getValue(Credentials.MERCHANT_ID))
                .ver(accessor.getValue(Credentials.VERSION))
                .method(Method.REGISTER.value)
                .username(context.getVendorPlayerUsername())
                .org(1)
                .ip(context.getIpAddress())
                .build();

        // Convert DTO to Map
        Map<String, Object> paramMap = objectMapper.convertValue(request, new TypeReference<Map<String, Object>>() {
        });
        // sort params by sequence "abc" and generate signature
        String signature = VendorUtil.getSignature(paramMap, accessor.getValue(Credentials.API_KEY));

        return MemberCreateRequest.builder()
                .pid(accessor.getValue(Credentials.MERCHANT_ID))
                .ver(accessor.getValue(Credentials.VERSION))
                .method(Method.REGISTER.value)
                .username(context.getVendorPlayerUsername())
                .org(1)
                .ip(context.getIpAddress())
                .sign(signature)
                .build();
    }

    @Override
    public boolean isSuccess(MemberCreateResponse response) {
        return response.isSuccess();
    }

    @Override
    public MediaType getContentType() {
        return MediaType.APPLICATION_JSON;
    }
}
