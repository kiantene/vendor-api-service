package com.nextgen.gameaggregator.vendor.queenmaker.api.gameurl;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.vendor.queenmaker.api.authorize.Authorize;
import com.nextgen.gameaggregator.vendor.queenmaker.api.authorize.AuthorizeDto;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.Credentials;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.EndPoints;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class GameUrlService implements GameUrl {

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials)
            throws InvalidVendorLineException, InvalidFormatException {

        // Split the gameCode into two parts based on the underscore character "_"
        String[] parts = gameSession.getVendorGameCode().split("_", 2);
        String gpcode = parts[0];
        String gcode = parts[1];

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("gpcode", gpcode);
        formData.add("gcode", gcode);
        formData.add("token", "");
        formData.add("lang", gameSession.getVendorLanguageCode());

        return formData;
    }

    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession)
            throws InvalidVendorLineException, InvalidVendorResponseException {

        String gameApiUrl = credentials.get(Credentials.GAME_API_URL);
        Optional.ofNullable(gameApiUrl).orElseThrow(InvalidVendorLineException::new);

        AuthorizeDto authorizeDto = Authorize.getToken(credentials, gameSession);

        formData.set("token", authorizeDto.getAuthtoken());

        URI uri = UriComponentsBuilder.fromUriString(gameApiUrl + EndPoints.GAME_URL)
                .queryParams(formData)
                .build()
                .encode()
                .toUri();

        String gameUrl = uri.toString();

        log.info("gameUrl : " + gameUrl);

        GameUrlVo gameUrlVo = new GameUrlVo(gameUrl);

        if (gameUrlVo.getGameUrl() == null) {
            throw new InvalidVendorResponseException( "Invalid vendor response" );
        }

        return gameUrlVo;
    }
}
