package com.nextgen.gameaggregator.game.launcher.lucky365;

import com.nextgen.core.security.signature.SigningStrategyType;
import com.nextgen.gameaggregator.core.engine.game.url.AbstractGameLaunchHandler;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchHandler;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import com.nextgen.gameaggregator.entity.ga.VendorLineCredential;
import com.nextgen.gameaggregator.game.launcher.lucky365.create.CreatePlayerService;
import com.nextgen.gameaggregator.vendor.lucky365.constant.Credentials;
import com.nextgen.gameaggregator.vendor.lucky365.constant.DeviceType;
import com.nextgen.gameaggregator.vendor.lucky365.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.lucky365.constant.Method;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.util.Locale;


@Service(EndPoints.CLASS_NAME + GameLaunchHandler.NAME)
public class lucky365GameLauncher extends AbstractGameLaunchHandler<GameLaunchRequest, GameLaunchResponse> {

    private final CreatePlayerService createPlayerService;

    protected lucky365GameLauncher(VendorCredentialUtils credentialUtils, CreatePlayerService createPlayerService) {
        super(credentialUtils, EndPoints.CLASS_NAME, GameLaunchResponse.class, SigningStrategyType.MD5);
        this.createPlayerService = createPlayerService;
    }


    @Override
    public String getBaseUrl(GameLaunchContext context) {
        VendorCredentialAccessor credentialAccessor = credentials(context.getVendorCredentials());
        VendorLineCredential urlSchemeCredential = credentialAccessor.get(Credentials.API_URL);
        return urlSchemeCredential.getValue();
    }

    @Override
    public String getPath(GameLaunchContext gameLaunchContext) {
        return EndPoints.GAME_URL_PATH;
    }

    @Override
    public GameLaunchRequest buildRequestBody(GameLaunchContext context) {

        createPlayerService.process(context);
        VendorCredentialAccessor accessor = credentials(context.getVendorCredentials());
        String sn = accessor.getValue(Credentials.SERIAL_NUM);
        String secretKey = accessor.getValue(Credentials.SECRET_KEY);
        String encryptString = context.getToken() + Method.LOGIN + sn + context.getVendorPlayerUsername() + secretKey;
        String signature = sign(encryptString, "").toUpperCase(Locale.ROOT);

        return GameLaunchRequest.builder()
                .sn(sn)
                .id(context.getToken())
                .method(Method.LOGIN)
                .loginId(context.getVendorPlayerUsername())
                .signature(signature)
                .appType(Integer.valueOf(context.getVendorPlatformCode()))
                .language(context.getVendorLanguageCode())
                .gameCode(context.getVendorGameCode())
                .callbackAddress(context.getLobbyUrl())
                .deviceType(DeviceType.ALL)
                .build();
    }

    @Override
    public boolean isSuccess(GameLaunchResponse gameLaunchResponse) {

        return gameLaunchResponse != null
                && gameLaunchResponse.getData() != null
                && gameLaunchResponse.getData().getLoginUrl() != null;
    }

    @Override
    public MediaType getContentType() {
        return MediaType.APPLICATION_JSON;
    }


    @Override
    public void onSuccess(GameLaunchContext context, GameLaunchResponse response) {
        VendorCredentialAccessor accessor = credentials(context.getVendorCredentials());
        String baseUrl = accessor.getValue(Credentials.DOMAIN_URL);
        String gameUrl = rebuildRequest(response.getData().getLoginUrl(), context.getVendorGameCode(), baseUrl);
        context.setGameUrl(gameUrl);
    }


    public static String rebuildRequest(String url, String gameCode, String baseUrl) {

        if (url == null || !url.contains("?")) {
            throw new IllegalArgumentException("Invalid Lucky365 launch url");
        }

        String query = url.substring(url.indexOf('?') + 1);

        int ampIndex = query.indexOf('&');
        if (ampIndex <= 0 || ampIndex == query.length() - 1) {
            throw new IllegalArgumentException("Invalid Lucky365 query format");
        }

        String authCode = query.substring(0, ampIndex);
        String params = query.substring(ampIndex + 1);

        return baseUrl + authCode
                + "&" + params
                + "&" + gameCode;
    }
}