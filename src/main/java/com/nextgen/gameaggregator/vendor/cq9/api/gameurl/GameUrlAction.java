package com.nextgen.gameaggregator.vendor.cq9.api.gameurl;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.operator.game.url.GameUrlVo;
import org.springframework.util.MultiValueMap;

import java.util.Map;

public class GameUrlAction implements GameUrl {
    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials) throws InvalidVendorLineException {
        return null;
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials) throws InvalidVendorLineException {
        return null;
    }
}
