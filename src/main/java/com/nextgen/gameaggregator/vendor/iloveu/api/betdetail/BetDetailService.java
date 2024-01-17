package com.nextgen.gameaggregator.vendor.iloveu.api.betdetail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.entity.ga.VendorLanguageCode;
import com.nextgen.gameaggregator.entity.ga.custom.IBetDetailUrlInfo;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import com.nextgen.gameaggregator.exception.RecordNotFoundException;
import com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrl;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.vendor.iloveu.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.iloveu.constant.Credentials;
import com.nextgen.gameaggregator.vendor.iloveu.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class BetDetailService implements BetDetailUrl {

    @Autowired
    RequestService requestService;

    @Autowired
    VendorService vendorService;

    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Override
    public MultiValueMap<String, String> formDataBuilder(Map<String, String> credentials, IBetDetailUrlInfo iBetDetailUrlInfo, VendorLanguageCode vendorLanguageCode)
            throws InvalidVendorLineException, InvalidFormatException, RecordNotFoundException {

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        VendorService vendorService = new VendorService();

        //setup form data
        formData.add("PlayerId", iBetDetailUrlInfo.getVendorUsername());
        formData.add("OrderId", iBetDetailUrlInfo.getExternalRoundId());

        return formData;
    }

    @Override
    public BetDetailUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials,
                               IBetDetailUrlInfo iBetDetailUrlInfo, VendorLanguageCode vendorLanguageCode)
            throws InvalidVendorResponseException, InvalidVendorLineException {
        BetDetailUrlVo responseVo = new BetDetailUrlVo();

        String detailUrl = credentials.get(Credentials.BET_DETAIL_URL);
        Optional.ofNullable(detailUrl).orElseThrow(InvalidVendorLineException::new);

        //convert from data into mapper data
        Map<String, String> convertFormMap = new HashMap<String, String>();
        convertFormMap.put("PlayerId", formData.getFirst("PlayerId"));
        convertFormMap.put("OrderId", formData.getFirst("OrderId"));

        //convert mapper data into json string
        String jsonFormString = "";
        String betDetail = "";
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            jsonFormString = objectMapper.writeValueAsString(convertFormMap);

            betDetail = vendorService.base64(jsonFormString);
        } catch (Exception exception) { // any other exception encountered
            throw new InvalidVendorLineException("Json Convert Failed");
        }

        //Construct the Game URL
        String betDetailUrl = UriComponentsBuilder.fromUriString(detailUrl)
                .path(EndPoints.BET_DETAIL_URL)
                .query(betDetail)
                .build()
                .encode()
                .toUri()
                .toString();

        responseVo.setUrl(betDetailUrl);

        return responseVo;
    }
}
