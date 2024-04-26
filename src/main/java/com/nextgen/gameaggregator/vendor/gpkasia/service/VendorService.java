package com.nextgen.gameaggregator.vendor.gpkasia.service;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.VendorGameCode;
import com.nextgen.gameaggregator.exception.GameNotSupportedException;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.VendorGameCodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class VendorService extends BaseVendorService {
    @Autowired
    private VendorGameCodeService vendorGameCodeService;

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

    public static long getMilSec(){
        return System.currentTimeMillis();
    }

    public void verifyVendorGameCode(GameSession gameSession, String gameId) throws GameNotSupportedException {
        VendorGameCode vendorGameCode = vendorGameCodeService.getByVendorGameIdAndPlatformIdAndLanguageId(gameSession.getVendorGameId(), gameSession.getPlatformId(), gameSession.getLanguageId());
        if (!vendorGameCode.getBetGameCode().equals(gameId)) {
            throw new GameNotSupportedException();
        }
    }

    public List<VendorGameCode> getVendorGameCode(GameSession gameSession, String gameId) throws GameNotSupportedException {
        List<VendorGameCode> vendorGameCodeList = vendorGameCodeService.getByBetGameCodeAndLanguageIdAndPlatformIdAndVendorId(gameId, gameSession.getLanguageId(), gameSession.getPlatformId(), gameSession.getVendorId());
        return vendorGameCodeList;
    }

    public static String trimGameCode(String gameCode){

        String trimmedGameCode = null;

        // check if game code contain _stg (ignore case-sensitive)
        if(gameCode.toLowerCase().contains("_stg")){
            // Trim value by removing _stg (ignore case-sensitive)
            trimmedGameCode = gameCode.replaceFirst("(?i)_stg$", "");
        }else{
            // let trimmedCode same as gameCode
            trimmedGameCode = gameCode;
        }

        return trimmedGameCode;
    }

    @Override
    public boolean shouldRejectCancelRequest() {
        return false;
    }
}
