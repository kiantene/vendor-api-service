package com.nextgen.gameaggregator.game.launcher.mtlive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.core.security.signature.SigningStrategyType;
import com.nextgen.gameaggregator.core.engine.game.url.AbstractGameLaunchHandler;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchHandler;
import com.nextgen.gameaggregator.core.exception.GameLaunchException;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import com.nextgen.gameaggregator.game.launcher.mtlive.member.create.MemberCreateService;
import com.nextgen.gameaggregator.vendor.mtlive.constant.Credentials;
import com.nextgen.gameaggregator.vendor.mtlive.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.mtlive.constant.Headers;
import com.nextgen.gameaggregator.vendor.mtlive.constant.ResponseCode;
import com.nextgen.gameaggregator.game.launcher.mtlive.util.VendorUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

@Service(EndPoints.CLASS_NAME + GameLaunchHandler.NAME)
public class MtliveGameLauncher extends AbstractGameLaunchHandler<MultiValueMap<String, String>, GameLaunchResponse> {
    private final MemberCreateService memberCreateService;

    public MtliveGameLauncher(VendorCredentialUtils credentialUtils,
                              MemberCreateService memberCreateService) {

        super(credentialUtils, EndPoints.CLASS_NAME, GameLaunchResponse.class, SigningStrategyType.MD5_REVERSE);
        this.memberCreateService = memberCreateService;
    }

    @Override
    public String getPath(GameLaunchContext context) {
        return EndPoints.LAUNCH_GAME;
    }

    @Override
    public String getBaseUrl(GameLaunchContext context) {
        VendorCredentialAccessor credentialAccessor = credentials(context.getVendorCredentials());
        return credentialAccessor.getValue(Credentials.API_URL);
    }

    @Override
    public AbstractGameLaunchHandler<MultiValueMap<String, String>, GameLaunchResponse> prepareLaunchRequest(GameLaunchContext context) {
        memberCreateService.process(context);
        return super.prepareLaunchRequest(context);
    }

    @Override
    public MultiValueMap<String, String> buildRequestBody(GameLaunchContext context) {
        VendorCredentialAccessor accessor = credentials(context.getVendorCredentials());
        String systemCode = accessor.getValue(Credentials.SYSTEM_CODE);
        String webId = accessor.getValue(Credentials.WEB_ID);
        String key = accessor.getValue(Credentials.DES_KEY);
        String iv  = accessor.getValue(Credentials.DES_IV);

        GameLaunchRequest request = GameLaunchRequest.builder()
                .systemCode(systemCode)
                .webId(webId)
                .userId(context.getVendorPlayerUsername())
                .language(context.getVendorLanguageCode())
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
    public boolean isSuccess(GameLaunchResponse response) {
        return Objects.equals(response.getCode(), ResponseCode.SUCCESS.getCode())
                && response.getData() != null
                && response.getData().getUrl() != null;
    }

    @Override
    public void onSuccess(GameLaunchContext context, GameLaunchResponse response) {
        context.setVendorToken(response.getData().getToken());
        context.setGameUrl(response.getData().getUrl());
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
}
