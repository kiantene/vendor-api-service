package com.nextgen.gameaggregator.game.launcher.evoplay;

import com.nextgen.gameaggregator.core.engine.game.url.AbstractGameLaunchHandler;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchHandler;
import com.nextgen.core.security.signature.SigningStrategyType;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import com.nextgen.gameaggregator.entity.ga.VendorLineCredential;
import com.nextgen.gameaggregator.vendor.evoplay.constant.Credentials;
import com.nextgen.gameaggregator.vendor.evoplay.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.evoplay.constant.Formats;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

// GA-14893 (interim): Evoplay is intentionally NOT registered as a new-framework game-launch
// handler. With no "evoplay" entry in GameLauncherRegistry, operator/game/url/GameUrlService falls
// through to the legacy vendor/evoplay/api/gameurl/GameUrlService, whose
// BaseGameUrlService.doGet().encode() correctly handles operator lobby URLs that contain a '#'
// fragment. The new-framework path fragment-splits/double-encodes such URLs (SC_VENDOR_ERROR).
//
// DO NOT simply uncomment @Service to switch back: getPath() below still uses .build(false) (no
// encoding), so re-registering this launcher WITHOUT the framework fix reintroduces the GA-14893
// bug (raw '#' fragment-splits the query -> denomination/currency dropped -> SC_VENDOR_ERROR).
// Re-enabling is owned by OVI-2598 and requires ALL of: the URI-aware shared caller, getPath()
// using .build().encode(), isUrlPreEncoded()=true, and the wire-level regression test.
// @Service(EndPoints.CLASS_NAME + GameLaunchHandler.NAME)
public class EvoplayGameLauncher extends AbstractGameLaunchHandler<String, GameLaunchResponse> {

    protected EvoplayGameLauncher(VendorCredentialUtils credentialUtils) {
        super(credentialUtils, EndPoints.CLASS_NAME, GameLaunchResponse.class, SigningStrategyType.MD5_REVERSE);
    }

    @Override
    public String getBaseUrl(GameLaunchContext context) {
        VendorCredentialAccessor credentialAccessor = credentials(context.getVendorCredentials());
        VendorLineCredential urlSchemeCredential = credentialAccessor.get(Credentials.API_URL);
        return urlSchemeCredential.getValue();
    }

    @Override
    public String getPath(GameLaunchContext context) {
        VendorCredentialAccessor credentialAccessor = credentials(context.getVendorCredentials());
        String key = credentialAccessor.getValue(Credentials.KEY);
        String projId = credentialAccessor.getValue(Credentials.PROJ_ID);

        String signature = generateSignature(context, projId, key);

        return UriComponentsBuilder.fromPath(EndPoints.GAME_URL)
                .queryParam("project", projId)
                .queryParam("version", "1")
                .queryParam("signature", signature)
                .queryParam("token", context.getToken())
                .queryParam("game", context.getVendorGameCode())
                .queryParam("settings[user_id]", context.getVendorPlayerUsername())
                .queryParam("settings[exit_url]", context.getLobbyUrl())
                .queryParam("settings[language]", context.getVendorLanguageCode())
                .queryParam("settings[https]", Formats.SETTINGS_HTTPS)
                .queryParam("denomination", Formats.DENOMINATION)
                .queryParam("currency", context.getVendorCurrencyCode())
                .queryParam("return_url_info", Formats.RETURN_URL_INFO)
                .queryParam("callback_version", Formats.CALLBACK_VERSION)
                .build(false)
                .toUriString();
    }

    @Override
    public String buildRequestBody(GameLaunchContext context) {
        return "";
    }

    private String generateSignature(GameLaunchContext context, String projId, String key) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("project", projId);
        params.add("version", "1");
        params.add("token", context.getToken());
        params.add("game", context.getVendorGameCode());
        params.add("settings[user_id]", context.getVendorPlayerUsername());
        params.add("settings[exit_url]", context.getLobbyUrl());
        params.add("settings[language]", context.getVendorLanguageCode());
        params.add("settings[https]", Formats.SETTINGS_HTTPS);
        params.add("denomination", Formats.DENOMINATION);
        params.add("currency", context.getVendorCurrencyCode());
        params.add("return_url_info", Formats.RETURN_URL_INFO);
        params.add("callback_version", Formats.CALLBACK_VERSION);

        String payload = buildSignature(params, key);
        return sign(payload, "");
    }

    @Override
    public HttpMethod getMethod() {
        return HttpMethod.GET;
    }

    @Override
    public MediaType getContentType() {
        return null;
    }

    @Override
    public boolean isSuccess(GameLaunchResponse gameLaunchResponse) {
        return gameLaunchResponse != null
                && gameLaunchResponse.getData() != null
                && gameLaunchResponse.getData().getLink() != null;
    }

    @Override
    public void onSuccess(GameLaunchContext context, GameLaunchResponse response) {
        String gameUrl = response.getData().getLink();
        context.setGameUrl(gameUrl);
    }

    private String buildSignature(MultiValueMap<String, String> mapData, String SignatureKey) {
        StringBuilder sb = new StringBuilder();
        for (String key : mapData.keySet()) {
            List<String> values = mapData.get(key);
            if (key.contains("[") || key.contains("]")) {
                sb.append(values.get(0));
                sb.append(":");
            } else {
                if (sb.length() > 0 && sb.charAt(sb.length() - 1) == ':') {
                    sb.setCharAt(sb.length() - 1, '*');
                }
                sb.append(values.get(0));
                sb.append("*");
            }
        }
        return sb.deleteCharAt(sb.length() - 1).append("*").append(SignatureKey).toString();
    }
}
