package com.nextgen.gameaggregator.game.launcher.koolbet;

import com.nextgen.gameaggregator.core.engine.game.url.AbstractGameLaunchHandler;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchHandler;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import com.nextgen.gameaggregator.entity.ga.VendorLineCredential;
import com.nextgen.gameaggregator.vendor.koolbet.constant.Credentials;
import com.nextgen.gameaggregator.vendor.koolbet.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.koolbet.service.VendorService;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

@Service(EndPoints.CLASS_NAME + GameLaunchHandler.NAME)
public class KoolbetGameLauncher extends AbstractGameLaunchHandler<String, GameLaunchResponse> {

    protected KoolbetGameLauncher(VendorCredentialUtils credentialUtils) {
        super(credentialUtils, EndPoints.CLASS_NAME, GameLaunchResponse.class);
    }

    @Override
    public void onSuccess(GameLaunchContext context, GameLaunchResponse response) {
        String gameUrl = response.getData();
        context.setGameUrl(gameUrl);
    }

    @Override
    public String getBaseUrl(GameLaunchContext context) {
        VendorCredentialAccessor credentialAccessor = credentials(context.getVendorCredentials());
        VendorLineCredential urlSchemeCredential = credentialAccessor.get(Credentials.API_URL);
        return urlSchemeCredential.getValue();
    }

    @Override
    public String getPath(GameLaunchContext context) {
        VendorCredentialAccessor accessor = credentials(context.getVendorCredentials());
        String agentId = accessor.getValue(Credentials.AGENT_ID);
        String apiToken = accessor.getValue(Credentials.API_TOKEN);

        //Construct Param
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("Token", context.getToken());
        params.add("GameId", context.getVendorGameCode());
        params.add("Lang", context.getVendorLanguageCode());

        //Encrypt param before sending
        String key = VendorService.generateKey(params, agentId, apiToken);
        return UriComponentsBuilder.fromPath(EndPoints.GAME_URL)
                .queryParam("Lang", context.getVendorLanguageCode())
                .queryParam("HomeUrl", context.getLobbyUrl())
                .queryParam("GameId", context.getVendorGameCode())
                .queryParam("Platform", context.getVendorPlatformCode())
                .queryParam("AgentId", agentId)
                .queryParam("Token", context.getToken())
                .queryParam("Key", key)
                .toUriString();
    }

    @Override
    public String buildRequestBody(GameLaunchContext context) {
        return "";
    }

    @Override
    public HttpMethod getMethod() {
        return HttpMethod.GET;
    }

    @Override
    public MediaType getContentType() {
        return MediaType.APPLICATION_JSON;
    }

    @Override
    public boolean isSuccess(GameLaunchResponse gameLaunchResponse) {
        return gameLaunchResponse != null
                && gameLaunchResponse.getData() != null;
    }
}
