package com.nextgen.gameaggregator.vendor.ifg.api.gameurl;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.VendorLineCredential;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.ifg.constant.Credentials;
import com.nextgen.gameaggregator.vendor.ifg.constant.GameType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@Service
public class GameUrlService implements GameUrl {

    @Autowired
    private VendorLineService vendorLineService;

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials)
            throws InvalidVendorLineException, InvalidFormatException {

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        int vendorLineId = gameSession.getVendorLineId();
        int version = getCredentialVersion(vendorLineId, Credentials.game_url);

        String vendorGameCode = gameSession.getVendorGameCode();
        String partner = credentials.get(Credentials.partner);
        String token = gameSession.getToken();
        String platform = gameSession.getVendorPlatformCode();
        String lang = gameSession.getVendorLanguageCode();

        if (version > 1) {
            formData.add("game", vendorGameCode);
            formData.add("project", partner);
            formData.add("auth", token);
            formData.add("platform", platform);
            formData.add("lang", lang);
            formData.add("demo", GameType.demo_false);
        } else {
            formData.add("partner", partner);
            formData.add("gameName", vendorGameCode);
            formData.add("platform", platform);
            formData.add("lang", lang);
            formData.add("demo", GameType.demo_false);
            formData.add("key", token);
        }

        return formData;
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession)
            throws InvalidVendorLineException {
        GameUrlVo responseVo = new GameUrlVo();

        //Get Vendor game URL
        String urlScheme = credentials.get(Credentials.game_url);

        //Construct the Game URL
        // Construct the Game URL
        URI uri = UriComponentsBuilder.fromUriString(urlScheme)
                .queryParams(formData)
                .build()
                .encode()
                .toUri();

        //Set the game URL and return to Operator
        responseVo.setGameUrl(uri.toString());

        return responseVo;
    }

    private Integer getCredentialVersion(Integer vendorLineId, String name) throws InvalidVendorLineException {
        VendorLineCredential vendorLineCredential = vendorLineService.getLatestCredentialByLineIdAndName(vendorLineId, name);

        if (vendorLineCredential == null) {
            throw new InvalidVendorLineException("VendorLineCredential not found for vendorLineId: " + vendorLineId + " and name: " + name);
        }   

        return vendorLineCredential.getVersion();
    }
}
