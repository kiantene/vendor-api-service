package com.nextgen.gameaggregator.vendor.jdb.api.gameurl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.service.BaseGameUrlService;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.vendor.jdb.constant.Actions;
import com.nextgen.gameaggregator.vendor.jdb.constant.Credentials;
import com.nextgen.gameaggregator.vendor.jdb.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.util.Map;

@Service
@Slf4j
public class GameUrlService extends BaseGameUrlService<GameUrlVo> {
    @Autowired
    RequestService requestService;

    @Value("${spring.profiles.active}")
    private String profilesActive;

    public GameUrlService() {
        super(GameUrlVo.class);
        this.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        this.setHttpMethod(HttpMethod.POST);
        this.setCredentialApiUrl(Credentials.API_SERVER);
        //this.setGameUrl();
    }

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession,
                                                         Map<String, String> credentials) throws InvalidVendorLineException, InvalidFormatException {

        // Split the gameCode into two parts based on the underscore character "_"
        //String[] parts = gameCode.split("_");
        String[] parts = gameSession.getVendorGameCode().split("_");
        int gType = Integer.parseInt(parts[0]);
        int mType = Integer.parseInt(parts[1]);

        String windowMode = "2"; // 2: Without using the JDB game lobby.

        GameUrlDto dto = new GameUrlDto();
        dto.setAction(Actions.GAME_URL);
        dto.setTs(System.currentTimeMillis());
        dto.setParent(credentials.get(Credentials.PARENT));
        dto.setUid(gameSession.getVendorPlayerUsername());
        dto.setBalance(BigDecimal.ZERO);
        dto.setLang(gameSession.getVendorLanguageCode());
        dto.setGType(gType);
        dto.setMType(String.valueOf(mType));
        dto.setWindowMode(windowMode);

        Gson gson = new GsonBuilder().create();
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();

        try {
            String x = VendorService.encrypt(gson.toJson(dto), credentials.get(Credentials.KEY), credentials.get(Credentials.IV));

            params.add("dc", credentials.get(Credentials.DC));
            params.add("x", x);

        } catch (Exception exception) {
            throw new InvalidFormatException(exception.getMessage());
        }

        return params;
    }

}
