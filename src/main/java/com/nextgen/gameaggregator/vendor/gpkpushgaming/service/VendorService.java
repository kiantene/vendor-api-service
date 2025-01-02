package com.nextgen.gameaggregator.vendor.gpkpushgaming.service;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.VendorGameCode;
import com.nextgen.gameaggregator.exception.GameNotSupportedException;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.VendorGameCodeService;
import lombok.Getter;
import lombok.Setter;
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
@Getter
@Setter
public class VendorService extends BaseVendorService {

    private final VendorGameCodeService vendorGameCodeService;
    private boolean settledByBet = false;

    @Autowired
    public VendorService(VendorGameCodeService vendorGameCodeService) {
        this.vendorGameCodeService = vendorGameCodeService;
    }

    public static long getCurrentTime() {
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

    public static long getMilSec() {
        return System.currentTimeMillis();
    }

    public static String trimGameCode(String gameCode) {

        String trimmedGameCode = null;

        // check if game code contain _stg (ignore case-sensitive)
        if (gameCode.toLowerCase().contains("_stg")) {
            // Trim value by removing _stg (ignore case-sensitive)
            trimmedGameCode = gameCode.replaceFirst("(?i)_stg$", "");
        } else {
            // let trimmedCode same as gameCode
            trimmedGameCode = gameCode;
        }

        return trimmedGameCode;
    }

    public void verifyVendorGameCode(GameSession gameSession, String gameId) throws GameNotSupportedException {
        VendorGameCode vendorGameCode = vendorGameCodeService.getByVendorGameIdAndPlatformIdAndLanguageId(gameSession.getVendorGameId(), gameSession.getPlatformId(), gameSession.getLanguageId());
        if (!vendorGameCode.getBetGameCode().equals(gameId)) {
            throw new GameNotSupportedException();
        }
    }

    public VendorGameCode getVendorGameCode(GameSession gameSession, String gameId) throws GameNotSupportedException {
        return vendorGameCodeService.getByBetGameCode(gameId, gameSession.getLanguageId(), gameSession.getPlatformId(), gameSession.getVendorId());
    }

    @Override
    public boolean shouldRejectCancelRequest() {
        return false;
    }

    @Override
    public boolean shouldDoRollbackByRound(GameSession gameSession) {
        // Handle for GPK BGAMING game, will be alwaus rollback by round
        if (gameSession.getGameCode().startsWith("GPKBG")) {
            return true;
        }
        return false;

    }

    @Override
    public boolean shouldSettleByBet() {
        // Temporary only BGAMING, SpadeGaming, EvoNetent need to accept cancel request
        return this.settledByBet;
    }
}
