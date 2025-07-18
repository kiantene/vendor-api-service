package com.nextgen.gameaggregator.game.launcher.aviatorstudio;

import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchHandler;
import com.nextgen.gameaggregator.core.engine.game.url.StaticHtmlGameLauncher;
import com.nextgen.gameaggregator.entity.ga.VendorLineCredential;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.Credentials;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.aviatorstudio.service.VendorService;
import org.springframework.stereotype.Component;

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
        String token;
        try {
            token = VendorService.generateJWT(userid, sessionId, jwtToken, publicKey);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        return null;
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
                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0, user-scalable=no, minimal-ui" />
                    <meta name="apple-mobile-web-app-capable" content="yes" />
                    <meta name="mobile-web-app-capable" content="yes" />
                    <meta name="apple-mobile-web-app-status-bar-style" content="black" />
                    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
                    <script src="https://cdn.inbetgames.com/gamelist_data.js"></script>
                    <script src="https://cdn.inbetgames.com/loader/build/app.js"></script>
                    <script type="text/javascript">
                        const path_to_storage = "{{pathToStorage}}";
                        const application = "{{application}}";
                        const customer_id = "{{customerId}}";
                        const session = "{{session}}";
                        const denomination = "{{denomination}}";
                        const kf_list = [{{kfList}}];
                        const bet_per_line = [{{betPerLine}}];
                        const currency = "{{currency}}";
                        const language = "{{language}}";
                        const home_page = "{{homePage}}";
                        window.onload = function () {
                            window.init_loader({
                                path: path_to_storage,
                                game: application,
                                billing: customer_id,
                                token: session,
                                kf: denomination,
                                currency: currency,
                                language: language,
                                home_page: home_page
                            });
                        };
                    </script>
                </head>
                <body>
                <div id="game-content"></div>
                </body>
                </html>
                """;
    }
}
