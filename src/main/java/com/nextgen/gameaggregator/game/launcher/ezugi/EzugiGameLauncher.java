package com.nextgen.gameaggregator.game.launcher.ezugi;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchHandler;
import com.nextgen.gameaggregator.core.engine.game.url.QueryStringUrlGameLauncher;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import com.nextgen.gameaggregator.entity.ga.VendorLineCredential;
import com.nextgen.gameaggregator.vendor.ezugi.constant.Credentials;
import com.nextgen.gameaggregator.vendor.ezugi.constant.EndPoints;

@Service(EndPoints.CLASS_NAME + GameLaunchHandler.NAME)
public class EzugiGameLauncher extends QueryStringUrlGameLauncher<GameLaunchRequest> {

    protected EzugiGameLauncher(VendorCredentialUtils credentialUtils) {
        super(credentialUtils, EndPoints.CLASS_NAME);
    }

     @Override
    public GameLaunchRequest buildRequestBody(GameLaunchContext context) {
        VendorCredentialAccessor credentialAccessor = credentials(context.getVendorCredentials());
        
        // Get category code list and check game code
        String categoryCodeList = credentialAccessor.getValue(Credentials.CATEGORY_CODE);
        String categoryCode = checkGameCodeIsOpenInCategoryLobby(categoryCodeList, context.getVendorGameCode());
        
        GameLaunchRequest.GameLaunchRequestBuilder builder = GameLaunchRequest.builder()
                .language(context.getVendorLanguageCode())
                .token(context.getToken())
                .operatorId(credentialAccessor.getValue(Credentials.OPERATOR_ID))
                .homeUrl(context.getLobbyUrl());
        
        if (categoryCode != null && !categoryCode.isBlank()) {
            builder.selectGame(context.getVendorGameCode());
        } else {
            builder.openTable(context.getVendorGameCode());
        }
        
        return builder.build();
    }

    @Override
    public String getBaseUrl(GameLaunchContext context) {
        VendorCredentialAccessor credentialAccessor = credentials(context.getVendorCredentials());
        VendorLineCredential urlSchemeCredential = credentialAccessor.get(Credentials.LOBBY_URL);
        return urlSchemeCredential.getValue();
    }

    @Override
    public String getPath(GameLaunchContext context) {
        return "";
    }

    private String checkGameCodeIsOpenInCategoryLobby(String categoryCodeList, String gameCode) {
        if (categoryCodeList != null && !categoryCodeList.isBlank()) {
            String[] elements = StringUtils.tokenizeToStringArray(categoryCodeList.trim(), ",");
            for (String element : elements) {
                if (element.equals(gameCode)) {
                    return gameCode;
                }
            }
        }
        return null;
    }
}
