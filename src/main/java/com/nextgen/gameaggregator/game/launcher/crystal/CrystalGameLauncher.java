package com.nextgen.gameaggregator.game.launcher.crystal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.core.engine.game.url.AbstractGameLaunchHandler;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchHandler;
import com.nextgen.gameaggregator.core.exception.GameLaunchException;
import com.nextgen.gameaggregator.core.signature.SigningStrategyType;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import com.nextgen.gameaggregator.entity.ga.VendorLineCredential;
import com.nextgen.gameaggregator.vendor.crystal.constant.Credentials;
import com.nextgen.gameaggregator.vendor.crystal.constant.EndPoints;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;

@Service(EndPoints.CLASS_NAME + GameLaunchHandler.NAME)
public class CrystalGameLauncher extends AbstractGameLaunchHandler<GameLaunchRequest, GameLaunchResponse> {

    public CrystalGameLauncher(VendorCredentialUtils credentialUtils) {
        super(credentialUtils, EndPoints.CLASS_NAME, GameLaunchResponse.class, SigningStrategyType.HMAC_SHA256);
    }

    private String convertToCompactJson(MultiValueMap<String, String> formData) {
        Map<String, String> dataMap = formData.toSingleValueMap();
        try {
            return new ObjectMapper().writeValueAsString(dataMap)
                    .replaceAll("\\s+", "");
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public String getBaseUrl(GameLaunchContext context) {
        VendorCredentialAccessor credentialAccessor = credentials(context.getVendorCredentials());
        VendorLineCredential urlSchemeCredential = credentialAccessor.get(Credentials.GAME_URL);
        return urlSchemeCredential.getValue();
    }

    @Override
    public String getPath(GameLaunchContext gameLaunchContext) {
        return EndPoints.LAUNCH_GAME;
    }

    @Override
    public GameLaunchRequest buildRequestBody(GameLaunchContext context) {

        VendorCredentialAccessor accessor = credentials(context.getVendorCredentials());
        String brandCode = accessor.getValue(Credentials.BRAND_CODE);

        return GameLaunchRequest.builder()
                .gameCode(context.getVendorGameCode())
                .brandCode(brandCode)
                .currencyCode(context.getVendorCurrencyCode())
                .playerId(context.getVendorPlayerUsername())
                .build();
    }

    @Override
    public boolean isSuccess(GameLaunchResponse gameLaunchResponse) {
        return gameLaunchResponse != null
                && gameLaunchResponse.getData() != null
                && gameLaunchResponse.getData().getUrl() != null;
    }

    @Override
    public MediaType getContentType() {
        return MediaType.APPLICATION_JSON;
    }

    @Override
    public Map<String, String> getHeaders(GameLaunchContext context, GameLaunchRequest requestObject) {
        VendorCredentialAccessor accessor = credentials(context.getVendorCredentials());
        String secretKey = accessor.getValue(Credentials.SECRET_KEY);
        String operatorCode = accessor.getValue(Credentials.OPERATOR_CODE);

        return Map.of(
                "X-SIGNATURE", sign(requestObject, secretKey),
                "OPERATOR", operatorCode
        );
    }

//    @Override
//    public Map<String, String> getHeaders(GameLaunchContext context, GameLaunchRequest requestObject) {
//        VendorCredentialAccessor accessor = credentials(context.getVendorCredentials());
//        String secretKey = accessor.getValue(Credentials.SECRET_KEY);
//        String operatorCode = accessor.getValue(Credentials.OPERATOR_CODE);
//        String signature;
//        try {
//            MultiValueMap<String, String> formData = this.formDataBuilder(context);
//            String compactJson = convertToCompactJson(formData);
//            signature = sign(compactJson, secretKey);
//
//        } catch (Exception e) {
//            throw new GameLaunchException(e.getMessage(), e);
//        }
//        return Map.of(
//                "X-SIGNATURE", signature,
//                "OPERATOR", operatorCode
//        );
//    }

//    private MultiValueMap<String, String> formDataBuilder(GameLaunchContext context) {
//        VendorCredentialAccessor accessor = credentials(context.getVendorCredentials());
//        String branCode = accessor.getValue(Credentials.BRAND_CODE);
//        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
//        formData.add("gameCode", context.getVendorGameCode());
//        formData.add("brandCode", branCode);
//        formData.add("currencyCode", context.getVendorCurrencyCode());
//        formData.add("playerId", context.getVendorPlayerUsername());
//        return formData;
//    }

    @Override
    public void onSuccess(GameLaunchContext context, GameLaunchResponse response) {
        String gameUrl = response.getData().getUrl();
        context.setGameUrl(gameUrl);
    }
}