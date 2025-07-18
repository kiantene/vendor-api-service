package com.nextgen.gameaggregator.game.launcher.aviatorstudio;

import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchHandler;
import com.nextgen.gameaggregator.core.engine.game.url.StaticHtmlGameLauncher;
import com.nextgen.gameaggregator.entity.ga.VendorLineCredential;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.Credentials;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.aviatorstudio.service.VendorService;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component(EndPoints.CLASS_NAME + GameLaunchHandler.NAME)
public class AviatorStudioGameLauncher extends StaticHtmlGameLauncher<GameLaunchRequest> {
    @Override
    public String getVendorClassName() {
        return EndPoints.CLASS_NAME;
    }

    @Override
    public GameLaunchRequest onPrepareRequestBody(GameLaunchContext context) {
        Map<String, VendorLineCredential> credentialMap = context.getVendorCredentials();
        String userid = context.getVendorPlayerUsername();
        String sessionId = context.getToken();
        String publicKey = getRequiredCredentialValue(credentialMap, Credentials.PUBLIC_KEY);
        String jwtToken = getRequiredCredentialValue(credentialMap, Credentials.JWT_SECRET);
        String encodedToken;
        try {
            String token = VendorService.generateJWT(userid, sessionId, jwtToken, publicKey);
            encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return GameLaunchRequest.builder()
                .token(encodedToken)
                .providerId(userid)
                .currency(context.getVendorCurrencyCode())
                .language(context.getVendorLanguageCode())
                .gameId(context.getVendorGameCode())
                .backtoHome(context.getLobbyUrl())
                .fullscreen(context.getVendorLanguageCode())
                .build();
    }

    @Override
    public void onSuccess(GameLaunchContext context, String response) {

    }

    @Override
    public String getHtmlTemplate() {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1" />
                    <meta name="viewport" content="width=device-width, height=device-height, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0, user-scalable=no, viewport-fit=cover, minimal-ui">
                    <meta name="apple-mobile-web-app-capable" content="yes" />
                    <meta name="mobile-web-app-capable" content="yes" />
                    <meta name="apple-mobile-web-app-status-bar-style" content="black" />
                    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
                </head>
                <body>
                <div id="game-content">
                    <iframe
                      allow="clipboard-read; clipboard-write"
                      src="https://client.staging.crash.aviator.studio:81?token="{{token}}"&providerId="{{providerId}}"&currency="{{currency}}"&language="{{language}}"&gameId="{{gameId}}"&backtoHome="{{backtoHome}}"&fullscreen="{{fullscreen}}"">
                    </iframe>
                </div>
                </body>
                </html>
                """;
    }
}
