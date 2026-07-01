package com.nextgen.gameaggregator.game.launcher.mtlive.member.create;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.core.security.signature.SigningStrategyType;
import com.nextgen.gameaggregator.core.engine.game.url.AbstractGameLaunchHandler;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.exception.GameLaunchException;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import com.nextgen.gameaggregator.game.launcher.mtlive.util.VendorUtil;
import com.nextgen.gameaggregator.vendor.mtlive.constant.Credentials;
import com.nextgen.gameaggregator.vendor.mtlive.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.mtlive.constant.Headers;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.Instant;
import java.util.Map;

@Component
public class MemberCreateHandler extends AbstractGameLaunchHandler<MultiValueMap<String, String>, MemberCreateResponse> {
    public MemberCreateHandler(VendorCredentialUtils credentialUtils) {
        super(credentialUtils, EndPoints.CLASS_NAME + "MemberCreateHandler", MemberCreateResponse.class, SigningStrategyType.MD5_REVERSE);
    }

    @Override
    public void onSuccess(GameLaunchContext context, MemberCreateResponse response) {
        //do nothing
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
    public MultiValueMap<String, String> buildRequestBody(GameLaunchContext context) {
        VendorCredentialAccessor accessor = credentials(context.getVendorCredentials());
        String systemCode = accessor.getValue(Credentials.SYSTEM_CODE);
        String webId = accessor.getValue(Credentials.WEB_ID);
        String key = accessor.getValue(Credentials.DES_KEY);
        String iv  = accessor.getValue(Credentials.DES_IV);

        MemberCreateRequest request = MemberCreateRequest.builder()
                .systemCode(systemCode)
                .webId(webId)
                .userId(context.getVendorPlayerUsername())
                .userName(context.getVendorPlayerUsername())
                .currency(context.getVendorCurrencyCode())
                .build();

        ObjectMapper mapper = new ObjectMapper();
        String encryptedMsg;
        try {
            encryptedMsg = VendorUtil.encrypt(mapper.writeValueAsString(request), key, iv);
        } catch (Exception ex) {
            throw new GameLaunchException(ex.getMessage(), ex);
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("msg", encryptedMsg);

        return form;
    }

    @Override
    public Map<String, String> getHeaders(GameLaunchContext context, MultiValueMap<String, String> requestObject) {
        String timeStamp = String.valueOf(Instant.now().getEpochSecond());
        VendorCredentialAccessor accessor = credentials(context.getVendorCredentials());
        String clientSecret = accessor.getValue(Credentials.CLIENT_SECRET);
        String clientId = accessor.getValue(Credentials.CLIENT_ID);

        String signature = sign(requestObject.getFirst("msg"), timeStamp+clientSecret+clientId);

        return Map.of(
                Headers.API_CI, clientId,
                Headers.API_SI, signature,
                Headers.API_TS, timeStamp
        );
    }

    @Override
    public boolean isSuccess(MemberCreateResponse response) {
        return response.isSuccess();
    }

}
