package com.nextgen.gameaggregator.game.launcher.saba;

import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchHandler;
import com.nextgen.gameaggregator.core.exception.InternalConfigurationException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.VendorLineCredential;
import com.nextgen.gameaggregator.vendor.saba.api.createmember.CreateMemberService;
import com.nextgen.gameaggregator.vendor.saba.constant.Credentials;
import com.nextgen.gameaggregator.vendor.saba.constant.EndPoints;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service(EndPoints.CLASS_NAME + "GameLauncher")
public class SabaGameLauncher implements GameLaunchHandler<GameLaunchRequest, GameLaunchResponse> {
    private static final ParameterizedTypeReference<GameLaunchResponse> RESPONSE_TYPE = new ParameterizedTypeReference<>() {};
    private static final String PLATFORM_WEB = "1";
    private static final String PLATFORM_H5 = "2";
    private static final String WEB_SKIN_TYPE = "&WebSkinType=";
    private static final String SKIN_TYPE = "&skin=";
    private static final String DEFAULT_SKIN_NEW_ASIA = "7";
    private final CreateMemberService createMemberService;

    public SabaGameLauncher(CreateMemberService createMemberService) {

        this.createMemberService = createMemberService;
    }

    @Override
    public String getVendorClassName() {
        return EndPoints.CLASS_NAME; // return null to disable this launcher
    }

    @Override
    public ParameterizedTypeReference<GameLaunchResponse> getResponseType() {
        return RESPONSE_TYPE;
    }

    @Override
    public String getPath() {
        return EndPoints.GAME_URL;
    }

    @Override
    public String getBaseUrl(GameLaunchContext context) {
        Map<String, VendorLineCredential> credentials = context.getVendorCredentials();

        return credentials.get(Credentials.API_URL).getValue();
    }

    @Override
    public GameLaunchRequest onPrepareRequestBody(GameLaunchContext context) {

        Map<String, VendorLineCredential> credentials = context.getVendorCredentials();
        String vendorId = Optional.ofNullable(credentials.get(Credentials.VENDOR_ID))
                .map(VendorLineCredential::getValue)
                .filter(value -> !value.isBlank())
                .orElseThrow(() -> new InternalConfigurationException(Credentials.VENDOR_ID + " is missing or has no value."));

        try { // requires refactor for createMember
            GameSession gameSession = new GameSession();
            gameSession.setVendorCurrencyCode(context.getVendorCurrencyCode());
            gameSession.setVendorPlayerUsername(context.getVendorPlayerUsername());
            createMemberService.call(gameSession, toCredentialsKV(credentials));
        } catch (Exception ex) {
            throw new RuntimeException(ex); // need better handling
        }

        return GameLaunchRequest.builder()
                .vendorId(vendorId)
                .vendorMemberId(context.getVendorPlayerUsername())
                .platform(context.getPlatformId() == 1 ? PLATFORM_H5 : PLATFORM_WEB)
                .build();
    }

    @Override
    public void onSuccess(GameLaunchContext context, GameLaunchResponse response) {
        String gameUrl = response.getData();
        Map<String, VendorLineCredential> credentials = context.getVendorCredentials();
        String skinParam = this.getSkinParamForUrl(credentials, context);
        String langParam = "&lang=" + context.getVendorLanguageCode();
        String homeUrlParam = "&homeUrl=" + context.getLobbyUrl();
        gameUrl = gameUrl + skinParam + langParam + homeUrlParam;

        context.setGameUrl(gameUrl);
    }

    private String getSkinParamForUrl(Map<String, VendorLineCredential> credentials, GameLaunchContext context) {
        String skinBase = context.getPlatformId().equals(1) ? SKIN_TYPE : WEB_SKIN_TYPE;
        String vendorCurrency = context.getVendorCurrencyCode();

        String customCurrency = getCredentialValue(credentials, Credentials.CUSTOM_CURRENCY, "");
        String customSkin = getCredentialValue(credentials, Credentials.CUSTOM_SKIN, DEFAULT_SKIN_NEW_ASIA);
        String defaultSkin = getCredentialValue(credentials, Credentials.DEFAULT_SKIN, DEFAULT_SKIN_NEW_ASIA);

        if (!customCurrency.isEmpty()) {
            List<String> currencyList = Arrays.asList(customCurrency.split(","));
            if (currencyList.contains(vendorCurrency)) {
                defaultSkin = customSkin;
            }
        }

        return skinBase + defaultSkin;
    }

    private Map<String, String> toCredentialsKV(Map<String, VendorLineCredential> credentialMap) {
        return credentialMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().getValue()
                ));
    }

    private String getCredentialValue(Map<String, VendorLineCredential> credentials, String key, String defaultValue) {
        return Optional.ofNullable(credentials.get(key))
                .map(VendorLineCredential::getValue)
                .orElse(defaultValue);
    }
}
