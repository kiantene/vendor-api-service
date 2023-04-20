package com.nextgen.gameaggregator.operator.game.url;

import com.nextgen.gameaggregator.entity.RawGameSession;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import org.springframework.util.MultiValueMap;

import java.util.Map;

public interface GameUrl {
    MultiValueMap<String, String> formDataBuilder(String gameCode, RawGameSession rawGameSession, Map<String, String> credentials)
            throws InvalidVendorLineException, InvalidFormatException;
    GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, RawGameSession rawGameSession) throws InvalidVendorLineException, InvalidVendorResponseException;
}
