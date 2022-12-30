package com.nextgen.gameaggregator.operator.game.url;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.operator.game.url.GameUrlVo;
import org.springframework.util.MultiValueMap;

import java.util.Map;

public interface GameUrl {
    MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials)
            throws InvalidVendorLineException;
    GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials) throws InvalidVendorLineException;
}
