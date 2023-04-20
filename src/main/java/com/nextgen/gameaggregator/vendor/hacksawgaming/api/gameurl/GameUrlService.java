package com.nextgen.gameaggregator.vendor.hacksawgaming.api.gameurl;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.RawGameSession;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.vendor.hacksawgaming.constant.Credentials;
import com.nextgen.gameaggregator.vendor.hacksawgaming.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.hacksawgaming.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import java.util.Map;
import java.util.Optional;


@Service
@Slf4j
public class GameUrlService implements GameUrl {
    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, RawGameSession rawGameSession, Map<String, String> credentials)
            throws InvalidVendorLineException, InvalidFormatException {

        String brandId = credentials.get(Credentials.BRAND_ID);
        Optional.ofNullable(brandId).orElseThrow(InvalidVendorLineException::new);
        String apiKey = credentials.get(Credentials.API_KEY);
        Optional.ofNullable(apiKey).orElseThrow(InvalidVendorLineException::new);
        String countryCode = credentials.get(Credentials.COUNTRY_CODE);
        Optional.ofNullable(countryCode).orElseThrow(InvalidVendorLineException::new);

        String brandUid = rawGameSession.getVendorPlayerUsername();
        String platform = "pc";
        if(!rawGameSession.getPlatformId().equals(2)) {
            platform = "mobile";
        }

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.set("brand_id", brandId);
        formData.set("sign", VendorService.getSign(brandId + brandUid + apiKey));
        formData.set("brand_uid", brandUid);
        formData.set("token", VendorService.removeDashes(rawGameSession.getToken()));
        formData.set("game_id", rawGameSession.getVendorGameCode());
        formData.set("currency", rawGameSession.getVendorCurrencyCode());
        formData.set("language", rawGameSession.getVendorLanguageCode());
        formData.set("channel", platform);
        formData.set("country_code", countryCode);

        return formData;
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, RawGameSession rawGameSession)
            throws InvalidVendorLineException, InvalidVendorResponseException {

        String apiUrl = credentials.get(Credentials.API_URL);
        Map<String, String> map = formData.toSingleValueMap();
        String json = new Gson().toJson(map);

        log.info("Calling " + apiUrl + EndPoints.GAME_URL);
        log.info("HSG GameUrlService: " + json);

        // create OkHttpClient instance
        OkHttpClient client = new OkHttpClient();

        // create request body
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, json);

        // create request
        Request request = new Request.Builder()
                .url(apiUrl + EndPoints.GAME_URL)
                .post(body)
                .build();

        try{
            // execute request
            Response response = client.newCall(request).execute();
            String responseBody = response.body().string();
            response.close();

            // log response data
            log.info(responseBody);

            // deserialize response body to DTO using Jackson
            ObjectMapper mapper = new ObjectMapper();
            GameUrlVendorResponseVo responseVo = mapper.readValue(responseBody, GameUrlVendorResponseVo.class);

            return responseVo.getData();

        } catch (Exception e) {
            throw new InvalidVendorResponseException("Invalid Response : " + e.getMessage());
        }

    }
}
