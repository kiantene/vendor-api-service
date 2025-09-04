package com.nextgen.gameaggregator.util;

import lombok.experimental.UtilityClass;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.Map;

@UtilityClass
public class MapUtils {

    /**
     * Converts MultiValueMap<String, String> to a Map<String, Object>,
     * ensuring the JSON structure matches that of a standard Map<String, Object>.
     * This is useful for serialization consistency.
     */
    public Map<String, Object> convertMultiValueMapToMap(MultiValueMap<String, String> multiValueMap) {
        Map<String, Object> resultMap = new HashMap<>();
        multiValueMap.forEach((key, valueList) -> {
            if (valueList != null && !valueList.isEmpty()) {
                resultMap.put(key, valueList.get(0)); // Take the first value
            }
        });
        return resultMap;
    }
}
