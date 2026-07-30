package com.nextgen.gameaggregator.game.launcher.vplus;

import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchHandler;
import com.nextgen.gameaggregator.core.engine.game.url.QueryStringUrlGameLauncher;
import com.nextgen.core.security.signature.SigningStrategyType;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import com.nextgen.gameaggregator.entity.ga.VendorLineCredential;
import com.nextgen.gameaggregator.game.launcher.vplus.member.create.MemberCreateService;
import com.nextgen.gameaggregator.game.launcher.vplus.member.login.MemberLoginService;
import com.nextgen.gameaggregator.game.launcher.vplus.util.VendorUtil;
import com.nextgen.gameaggregator.vendor.vplus.constant.Credentials;
import com.nextgen.gameaggregator.vendor.vplus.constant.EndPoints;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@Service(EndPoints.CLASS_NAME + GameLaunchHandler.NAME)
public class VplusGameLauncher extends QueryStringUrlGameLauncher<GameLaunchRequest> {
    private static final String APP_ID = "appId";
    private static final String APP_SECRET = "appSecret";
    private static final String APP_URL = "apiUrl";
    private static final String TIME_STAMP = "timestamp";
    private static final String USERNAME = "username";
    private static final String CLOSE_BACK = "1";
    private final MemberCreateService memberCreateService;
    private final MemberLoginService memberLoginService;

    public VplusGameLauncher(VendorCredentialUtils credentialUtils, MemberCreateService memberCreateService, MemberLoginService memberLoginService) {
        super(credentialUtils, EndPoints.CLASS_NAME, SigningStrategyType.MD5);
        this.memberCreateService = memberCreateService;
        this.memberLoginService = memberLoginService;
    }

    @Override
    public String getBaseUrl(GameLaunchContext context) {
        VendorCredentialAccessor credentialAccessor = credentials(context.getVendorCredentials());
        VendorLineCredential urlSchemeCredential = credentialAccessor.get(Credentials.GAME_LAUNCH);
        return urlSchemeCredential.getValue();
    }

    @Override
    public String getPath(GameLaunchContext gameLaunchContext) {
        return "";
    }

    @Override
    public void onSuccess(GameLaunchContext context, String response) {
        context.setVendorToken(TokenHolder.getToken());
        TokenHolder.clear();
        context.setGameUrl(response);
    }

    @Override
    public GameLaunchRequest buildRequestBody(GameLaunchContext context) {
        memberCreateService.process(context);
        memberLoginService.process(context);

        VendorCredentialAccessor accessor = credentials(context.getVendorCredentials());
        // prepare params
        Map<String, String> params = this.prepareRequestParams(context.getVendorPlayerUsername(), accessor);

        String lang = context.getVendorLanguageCode();

        // sort params by sequence "abc"
        Map<String, String> sortedParams = this.buildSortedParams(params, TokenHolder.getToken(), context, lang);
        // arrange the sortedParams to query string
        String queryParams = VendorUtil.generateSign(sortedParams);

        GameLaunchRequest.GameLaunchRequestBuilder requestBuilder = GameLaunchRequest.builder()
                .appId(params.get(APP_ID))
                .timestamp(params.get(TIME_STAMP))
                .sign(sign(queryParams, params.get(APP_SECRET)))
                .token(TokenHolder.getToken())
                .id(context.getVendorGameCode())
                .closeBack(CLOSE_BACK);

        if (lang != null) {
            requestBuilder.lang(lang);
        }

        return requestBuilder.build();
    }

    private Map<String, String> buildSortedParams(Map<String, String> params, String loginToken, GameLaunchContext context, String lang) {
        Map<String, String> sortedParams = new TreeMap<>();
        sortedParams.put(APP_ID, params.get(APP_ID));
        sortedParams.put(TIME_STAMP, params.get(TIME_STAMP));
        sortedParams.put("token", loginToken);
        sortedParams.put("id", context.getVendorGameCode());
        sortedParams.put("closeBack", CLOSE_BACK);
        if (lang != null) {
            sortedParams.put("lang", lang);
        }
        return sortedParams;
    }

    private Map<String, String> prepareRequestParams(String vendorPlayerUsername, VendorCredentialAccessor accessor) {
        Map<String, String> params = new HashMap<>();
        params.put(APP_URL, accessor.getValue(APP_URL));
        params.put(APP_ID, accessor.getValue(APP_ID));
        params.put(APP_SECRET, accessor.getValue(APP_SECRET));
        params.put(USERNAME, vendorPlayerUsername);
        params.put(TIME_STAMP, String.valueOf(System.currentTimeMillis()));
        return params;
    }
}