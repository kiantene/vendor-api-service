package com.nextgen.gameaggregator.vendor.yeebet.api.betdetail;

import com.nextgen.gameaggregator.entity.ga.VendorLanguageCode;
import com.nextgen.gameaggregator.entity.ga.custom.IBetDetailUrlInfo;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import com.nextgen.gameaggregator.exception.RecordNotFoundException;
import com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrl;
import com.nextgen.gameaggregator.vendor.yeebet.constant.Credentials;
import com.nextgen.gameaggregator.vendor.yeebet.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.yeebet.service.VendorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Optional;

public class BetDetailService implements BetDetailUrl {

    @Autowired
    VendorService vendorService;

    @Override
    public MultiValueMap<String, String> formDataBuilder(Map<String, String> credentials, IBetDetailUrlInfo iBetDetailUrlInfo, VendorLanguageCode vendorLanguageCode)
            throws InvalidVendorLineException, InvalidFormatException, RecordNotFoundException {

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        //setup form data
        formData.add("appid", credentials.get(Credentials.game_app_id));
        formData.add("ids", iBetDetailUrlInfo.getTransactionId());
        formData.add("index", "0");
        formData.add("size", "2000");

        //hash all the data to generate sign value
        formData.add("sign", vendorService.generateSign(formData, credentials.get(Credentials.game_secret_key)));

        return formData;
    }

    @Override
    public BetDetailUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, IBetDetailUrlInfo iBetDetailUrlInfo, VendorLanguageCode vendorLanguageCode)
            throws InvalidVendorResponseException, InvalidVendorLineException {

        BetDetailUrlVo responseVo = new BetDetailUrlVo();

        //construct API address
        String urlScheme = credentials.get(Credentials.api_url);

        //check vendor status in our DB
        Optional.ofNullable(urlScheme).orElseThrow(InvalidVendorLineException::new);

        //Construct the API to get game url from vendor site(those parameter get from formDataBuilder function)
        String uri = UriComponentsBuilder.fromUriString(urlScheme)
                .path(EndPoints.BET_DETAIL_URL)
                .queryParams(formData)
                .build()
                .encode()
                .toUri()
                .toString();

        responseVo.setUrl(uri);

        return responseVo;
    }
}
