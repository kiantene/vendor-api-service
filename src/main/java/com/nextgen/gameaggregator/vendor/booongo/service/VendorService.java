package com.nextgen.gameaggregator.vendor.booongo.service;

import com.nextgen.gameaggregator.service.BaseVendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class VendorService extends BaseVendorService {
    public static String generateGameUrl(String API_URL, String PROJECT_NAME, String token, String platform, String gameCode, String lang){
        String gameUrl = API_URL + PROJECT_NAME + "/game.html";

        //combine those string to form url
        gameUrl = gameUrl + "?token=" + token + "&platform=" + platform + "&game=" + gameCode + "&lang=" + lang;

//        gameUrl = MessageFormat.format(gameUrl, token, platform, gameCode, lang);

        return gameUrl;
    }
}
