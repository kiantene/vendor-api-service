package com.nextgen.gameaggregator.vendor.queenmaker.api.authorize;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.RawGameSession;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.Credentials;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.Formats;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.http.HttpHeaders;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Data
@Slf4j
public class Authorize {

    public static AuthorizeDto getToken(Map<String, String> credentials, RawGameSession rawGameSession)
            throws  InvalidVendorLineException,
                    InvalidVendorResponseException {

        String clientId = credentials.get(Credentials.CLIENT_ID);
        Optional.ofNullable(clientId).orElseThrow(InvalidVendorLineException::new);

        String clientSecret = credentials.get(Credentials.CLIENT_SECRET);
        Optional.ofNullable(clientSecret).orElseThrow(InvalidVendorLineException::new);

        String apiUrl = credentials.get(Credentials.API_URL);
        Optional.ofNullable(apiUrl).orElseThrow(InvalidVendorLineException::new);

        String ipAddress = credentials.get(Credentials.WHITELISTED_IP_ADDRESS);
        Optional.ofNullable(ipAddress).orElseThrow(InvalidVendorLineException::new);

        Map<String, Object> formData = new LinkedHashMap<>();
        formData.put("ipaddress", ipAddress);
        formData.put("username", rawGameSession.getVendorPlayerUsername());
        formData.put("userid", rawGameSession.getVendorPlayerUsername());
        formData.put("lang", rawGameSession.getVendorLanguageCode());
        formData.put("cur", rawGameSession.getVendorCurrencyCode());
        formData.put("betlimitid", Formats.BRONZE);
        formData.put("istestplayer", Formats.REAL_PLAYER);
        formData.put("platformtype", Integer.parseInt(rawGameSession.getVendorPlatformCode()));

        String jsonBody = new Gson().toJson(formData);

        OkHttpClient client = new OkHttpClient();

        RequestBody body = RequestBody.create(MediaType.parse(Formats.APPLICATION_JSON), jsonBody);

        Request request = new Request.Builder()
                .url(apiUrl + EndPoints.AUTHORIZE)
                .post(body)
                .addHeader(HttpHeaders.CONTENT_TYPE, Formats.APPLICATION_JSON)
                .addHeader(HttpHeaders.ACCEPT, Formats.APPLICATION_JSON)
                .addHeader(Formats.HEADER_CLIENT_ID, clientId)
                .addHeader(Formats.HEADER_CLIENT_SECRET, clientSecret)
                .build();

        log.info("Calling " + apiUrl + EndPoints.AUTHORIZE);
        log.info(jsonBody);

        String responseString = "";
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                responseString = Objects.requireNonNull(response.body()).string();
            } else {
                String responseBody = response.body().string();
                throw new InvalidVendorResponseException(responseBody);
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new InvalidVendorLineException("Fail to call API : " + apiUrl + EndPoints.AUTHORIZE);
        }

        AuthorizeDto authorizeDto = null;
        try {
            authorizeDto = new Gson().fromJson(responseString, AuthorizeDto.class);
        } catch (JsonSyntaxException jsonSyntaxException) {
            throw new InvalidVendorResponseException("Invalid vendor response body :" + responseString);
        }

        return authorizeDto;
    }
}
