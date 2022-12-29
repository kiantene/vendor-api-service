package com.nextgen.gameaggregator.vendor.pgsoft.api.gameurl;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.VendorPlayer;
import com.nextgen.gameaggregator.exception.CredentialNotFoundException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.Credentials;
import com.nextgen.gameaggregator.vendor.pgsoft.service.VendorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;
import java.util.UUID;

@Service
public class GameUrlAction implements GameUrl {

    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials) throws InvalidVendorLineException {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("gameCode", gameCode);
        formData.add("language", gameSession.getLanguage());
        formData.add("token", gameSession.getToken());
        return formData;
    }

    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials) throws InvalidVendorLineException {
        GameUrlVo responseVo = new GameUrlVo();

        // 4. Get Vendor URL Scheme by vendorPlayer's vendor_line_id
        String urlScheme = credentials.get(Credentials.URL_SCHEME);
        // 5. Lookup Game Code
        String gameCode = formData.get("gameCode").get(0);
        String language = formData.get("language").get(0);
        // 6. Generate game session token to embed into urlScheme and save into game session table
        String token = formData.get("token").get(0);
        // 7. Retrieve Operator Token as it is required to form the Game URL
        String operatorToken = credentials.get(Credentials.OPERATOR_TOKEN);
        // 8. Construct the Game URL
        String gameUrl = VendorService.generateGameUrl(urlScheme, gameCode, language, operatorToken, token);
        // 9. Save this player's game session
        // Set the game URL and return to Operator
        responseVo.setGameUrl(gameUrl);
//        responseVo.setGameUrl("https://m.pg-redirect.net/128/index.html?l=zh-cn&btt=1&ot=2f251cc0df2cc59265fe0e50ddc73e16&ops=55e9a80e-d8ce-4f63-b0a6-550aa4558f1");

        return responseVo;
    }

}
