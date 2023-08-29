package com.nextgen.gameaggregator.vendor.habanero.api.betdetail;

import com.nextgen.gameaggregator.entity.VendorLanguageCode;
import com.nextgen.gameaggregator.entity.custom.IBetDetailUrlInfo;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import com.nextgen.gameaggregator.exception.RecordNotFoundException;
import com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrl;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.vendor.habanero.constant.Credentials;
import com.nextgen.gameaggregator.vendor.habanero.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.habanero.service.VendorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
public class BetDetailService implements BetDetailUrl {

    @Autowired
    RequestService requestService;

    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Override
    public MultiValueMap<String, String> formDataBuilder(Map<String, String> credentials, IBetDetailUrlInfo iBetDetailUrlInfo, VendorLanguageCode vendorLanguageCode)
            throws InvalidVendorLineException, InvalidFormatException, RecordNotFoundException {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        //prepare hash value
        String brandId = credentials.get(Credentials.BRAND_ID);
        String gameInstanceId = iBetDetailUrlInfo.getExternalRoundId();
        String hashString = (gameInstanceId.toLowerCase() + brandId.toLowerCase() + credentials.get(Credentials.API_KEY).toLowerCase());

        //generate hash value
        String hash = "";
        try {
            hash = VendorService.generateSHA256Hash(hashString);
        } catch (Exception exception) { // any other exception encountered
            throw new InvalidVendorLineException("Hash Failed");
        }

        formData.add("brandid", brandId);
        formData.add("gameinstanceid", gameInstanceId);
        formData.add("hash", hash);
        formData.add("locale", vendorLanguageCode.getLanguageCode());
        formData.add("viewtype", "game");
        return formData;
    }

    @Override
    public BetDetailUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials,
                               IBetDetailUrlInfo iBetDetailUrlInfo, VendorLanguageCode vendorLanguageCode)
            throws InvalidVendorResponseException, InvalidVendorLineException {
        BetDetailUrlVo responseVo = new BetDetailUrlVo();

        //Construct the Game URL
        String gameUrl = UriComponentsBuilder.fromUriString(credentials.get(Credentials.API_URL))
                .path(EndPoints.BET_DETAIL_URL)
                .queryParams(formData)
                .build()
                .encode()
                .toUri()
                .toString();

        //Save this player's game session
        //Set the game URL and return to Operator
        responseVo.setUrl(gameUrl);

        return responseVo;
    }

}
