package com.nextgen.gameaggregator.vendor.ezugi.service;

import com.nextgen.gameaggregator.service.BaseVendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
@Service
@Slf4j
public class VendorService extends BaseVendorService {
    public static String generateGameUrl(String lobbyUrl, String playerGameSessionToken, String operatorId, String languageCode, String gameCode) {
        // form query string
        String loginUrl = lobbyUrl + "?token=" + playerGameSessionToken + "&operatorId=" + operatorId + "&language" + languageCode + "&selectGame=" + gameCode;
        return loginUrl;
    }
}
