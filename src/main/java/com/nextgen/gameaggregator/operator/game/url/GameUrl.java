package com.nextgen.gameaggregator.operator.game.url;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import org.springframework.util.MultiValueMap;

import java.util.Map;
import java.util.concurrent.TimeoutException;

public interface GameUrl {

    MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials)
            throws InvalidVendorLineException, InvalidFormatException;

    GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession)
            throws InvalidVendorLineException, InvalidVendorResponseException;

    default GameUrlVo callToVendor(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession, HttpRequestLog httpRequestLog)
            throws InvalidVendorLineException, InvalidVendorResponseException, TimeoutException {

        return this.call(formData, credentials, gameSession);
    }
}
