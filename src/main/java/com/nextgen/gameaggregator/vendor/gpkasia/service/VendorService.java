package com.nextgen.gameaggregator.vendor.gpkasia.service;

import com.nextgen.gameaggregator.service.BaseVendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class VendorService extends BaseVendorService {
    public static long getCurrentTime(){
        return Instant.now().getEpochSecond();
    }

    public static Map<String, Object> convertToHashMap(MultiValueMap<String, String> multiValueMap) {
        Map<String, Object> hashMap = new HashMap<>();

        // Iterate over entries in the MultiValueMap
        for (Map.Entry<String, List<String>> entry : multiValueMap.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();
            // Convert the list of values into an Object, e.g., by selecting the first value
            Object value = (values != null && !values.isEmpty()) ? (Object) values.get(0) : null;
            hashMap.put(key, value);
        }

        return hashMap;
    }

//    @Override
//    public boolean shouldRejectCancelRequest() {
//        return false;
//    }
}
