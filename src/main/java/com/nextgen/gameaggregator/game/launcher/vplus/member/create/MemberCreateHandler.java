package com.nextgen.gameaggregator.game.launcher.vplus.member.create;

import com.nextgen.gameaggregator.core.engine.game.url.AbstractGameLaunchHandler;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.core.security.signature.SigningStrategyType;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import com.nextgen.gameaggregator.game.launcher.vplus.util.VendorUtil;
import com.nextgen.gameaggregator.vendor.vplus.constant.Credentials;
import com.nextgen.gameaggregator.vendor.vplus.constant.EndPoints;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
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
        // prepare request params
        Map<String, String> params = new HashMap<>();
        params.put("apiUrl", accessor.getValue(Credentials.API_URL));
        params.put("appId", accessor.getValue(Credentials.APP_ID));
        params.put("appSecret", accessor.getValue(Credentials.APP_SECRET));
        params.put("username", context.getVendorPlayerUsername());
        params.put("timestamp", String.valueOf(System.currentTimeMillis()));
        // sort params by sequence "abc" and generate signature
        String queryParams = VendorUtil.generateSign(VendorUtil.sortedParams(params));

        return MemberCreateRequest.builder()
                .appId(accessor.getValue(Credentials.APP_ID))
                .timestamp(params.get("timestamp"))
                .sign(sign(queryParams, accessor.getValue(Credentials.APP_SECRET)))
                .username(context.getVendorPlayerUsername())
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
