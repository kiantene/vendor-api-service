package com.nextgen.gameaggregator.vendor.live22.api.gameurl;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.service.BaseGameUrlService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.live22.constant.Credentials;
import com.nextgen.gameaggregator.vendor.live22.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.live22.constant.Formats;
import com.nextgen.gameaggregator.vendor.live22.service.VendorService;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class GameUrlService extends BaseGameUrlService<GameUrlVo> {

    public GameUrlService() {
        super(GameUrlVo.class);
        this.setCredentialApiUrl(Credentials.GAME_URL);
        this.setGameUrl(EndPoints.GAME_URL);
    }

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials) throws InvalidVendorLineException {

        // Get the current date and time in UTC
        ZonedDateTime currentDateTime = ZonedDateTime.now(ZoneId.of(Formats.TIME_ZONE));
        // Define the formatter for the desired output pattern
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Formats.DATE_FORMAT);
        // Format the current date and time using the formatter
        String formattedDateTime = currentDateTime.format(formatter);

        String functionName = "GameLogin";
        String playerId = gameSession.getVendorPlayerUsername();

        String operatorId = ValidationUtils.validateCredential(credentials.get(Credentials.USERNAME));
        String secretKey = ValidationUtils.validateCredential(credentials.get(Credentials.SECRET_KEY));

        //generate encryptString
        String encryptString = functionName + formattedDateTime + operatorId + secretKey + playerId;

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("OperatorId", operatorId);
        formData.add("RequestDateTime", formattedDateTime);
        formData.add("PlayerId", playerId);
        formData.add("Ip", gameSession.getIpAddress());
        formData.add("GameCode", gameSession.getVendorGameCode());
        formData.add("Currency", gameSession.getVendorCurrencyCode());
        formData.add("Lang", gameSession.getVendorLanguageCode());
        formData.add("RedirectUrl", gameSession.getLobbyUrl());
        formData.add("AuthToken", gameSession.getToken());

        //hash all the data to generate sign value
        formData.add("Signature", VendorService.generateSign(encryptString));

        return formData;
    }
}
