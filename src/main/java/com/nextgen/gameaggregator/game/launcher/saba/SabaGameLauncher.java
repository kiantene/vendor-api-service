package com.nextgen.gameaggregator.game.launcher.saba;

import com.nextgen.gameaggregator.core.engine.game.url.AbstractGameLaunchHandler;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchHandler;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import com.nextgen.gameaggregator.entity.ga.VendorLineCredential;
import com.nextgen.gameaggregator.vendor.saba.constant.Credentials;
import com.nextgen.gameaggregator.vendor.saba.constant.EndPoints;
import org.springframework.stereotype.Service;

import java.util.*;

@Service(EndPoints.CLASS_NAME + GameLaunchHandler.NAME)
public class SabaGameLauncher extends AbstractGameLaunchHandler<GameLaunchRequest, GameLaunchResponse> {
    private static final String PLATFORM_WEB = "1";
    private static final String PLATFORM_H5 = "2";
    private final MemberCreateService memberCreateService;

    public SabaGameLauncher(VendorCredentialUtils credentialUtils,
                            MemberCreateService memberCreateService) {

        super(credentialUtils, EndPoints.CLASS_NAME, GameLaunchResponse.class);
        this.memberCreateService = memberCreateService;
    }

    @Override
    public String getPath(GameLaunchContext context) {
        return EndPoints.GAME_URL;
    }

    @Override
    public String getBaseUrl(GameLaunchContext context) {
        VendorCredentialAccessor credentialAccessor = credentials(context.getVendorCredentials());
        return credentialAccessor.getValue(Credentials.API_URL);
    }

    @Override
    public AbstractGameLaunchHandler<GameLaunchRequest, GameLaunchResponse> prepareLaunchRequest(GameLaunchContext context) {
        memberCreateService.process(context);
        return super.prepareLaunchRequest(context);
    }

    @Override
    public GameLaunchRequest buildRequestBody(GameLaunchContext context) {
        VendorCredentialAccessor credentialAccessor = credentials(context.getVendorCredentials());
        String vendorId = credentialAccessor.getValue(Credentials.VENDOR_ID);

        return GameLaunchRequest.builder()
                .vendorId(vendorId)
                .vendorMemberId(context.getVendorPlayerUsername())
                .platform(context.getPlatformId() == 1 ? PLATFORM_H5 : PLATFORM_WEB)
                .build();
    }

    @Override
    public boolean isSuccess(GameLaunchResponse response) {
        return response.getData() != null && !response.getData().isBlank();
    }

    @Override
    public void onSuccess(GameLaunchContext context, GameLaunchResponse response) {
        String gameUrl = response.getData();
        Integer agentId = context.getAgentId();
        String lobbyUrl = context.getLobbyUrl();
        String language = context.getVendorLanguageCode();

        boolean isHomeUrlDisabled = isAgentIdInDisabledList(agentId, context.getVendorCredentials());
        String homeUrl = isHomeUrlDisabled ? "" : "&homeUrl=" + lobbyUrl;

        String fullUrl = gameUrl +
                resolveSkinParam(context) +
                "&lang=" + language +
                homeUrl;

        context.setGameUrl(fullUrl);
    }


    private String resolveSkinParam(GameLaunchContext context) {
        VendorCredentialAccessor accessor = new VendorCredentialAccessor(context.getVendorCredentials());
        final String WEB_SKIN_TYPE = "&WebSkinType=";
        final String SKIN_TYPE = "&skin=";
        final String DEFAULT_SKIN_NEW_ASIA = "7";
        String skinPrefix = context.getPlatformId().equals(1) ? SKIN_TYPE : WEB_SKIN_TYPE;
        String vendorCurrency = context.getVendorCurrencyCode();

        String customCurrency = accessor.getOrDefault(Credentials.CUSTOM_CURRENCY, "");
        String customSkin = accessor.getOrDefault(Credentials.CUSTOM_SKIN, DEFAULT_SKIN_NEW_ASIA);
        String defaultSkin = accessor.getOrDefault(Credentials.DEFAULT_SKIN, DEFAULT_SKIN_NEW_ASIA);

        if (!customCurrency.isEmpty()) {
            List<String> currencyList = Arrays.asList(customCurrency.split(","));
            if (currencyList.contains(vendorCurrency)) {
                defaultSkin = customSkin;
            }
        }

        return skinPrefix + defaultSkin;
    }

    private boolean isAgentIdInDisabledList(Integer agentId, Map<String, VendorLineCredential> credentials) {
        try {
            String rawValue = Optional.ofNullable(credentials.get("agentId_disable_homepage_list"))
                    .map(VendorLineCredential::getValue)
                    .orElse("");

            return Arrays.stream(rawValue.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> {
                        try {
                            return Integer.parseInt(s);
                        } catch (NumberFormatException e) {
                            return null; // safely ignore bad values
                        }
                    })
                    .filter(Objects::nonNull)
                    .anyMatch(id -> id.equals(agentId));

        } catch (Exception e) {
            // If any unexpected exception occurs, return false
            return false;
        }
    }

}
