package com.nextgen.gameaggregator.vendor.spribe.api.betdetail;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.nextgen.gameaggregator.entity.VendorLanguageCode;
import com.nextgen.gameaggregator.entity.custom.IBetDetailUrlInfo;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrl;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.service.VendorLineService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BetDetailService implements BetDetailUrl {

    @Autowired
    RequestService requestService;

    @Autowired
    VendorLineService vendorLineService;

    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Override
    public MultiValueMap<String, String> formDataBuilder(Map<String, String> credentials,
            IBetDetailUrlInfo iBetDetailUrlInfo, VendorLanguageCode vendorLanguageCode)
            throws InvalidVendorLineException, InvalidFormatException, RecordNotFoundException {

            // Get operator and token by vendor line
            String operator = "";
            try {
                operator = vendorLineService.getCredentialValueByName(iBetDetailUrlInfo.getVendorLineId(), "operator");
            } catch (CredentialNotFoundException e) {
                log.error("Credential not found : " + e.getMessage());
            }
                
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("round_id", iBetDetailUrlInfo.getExternalRoundId());
            formData.add("game", iBetDetailUrlInfo.getGameCode());
            formData.add("player_token", "");
            formData.add("op_player_id", iBetDetailUrlInfo.getVendorUsername());
            formData.add("operator", operator);

            return formData;
    }

    @Override
    public BetDetailUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, IBetDetailUrlInfo iBetDetailUrlInfo, VendorLanguageCode vendorLanguageCode)
        throws InvalidVendorResponseException, InvalidVendorLineException {
            
        // Wait to fix GA-3448
        // Retrieve the API domain from the credentials map.
        // String apiUrl = credentials.getOrDefault(Credentials.API_URL, "");
        // if (apiUrl.isBlank()) {
        //     throw new InvalidVendorLineException();
        // }

        // // Build uri with formData
        // UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(apiUrl)
        //     .queryParam("round_id", formData.getFirst("round_id"))
        //     .queryParam("game", formData.getFirst("game"))
        //     .queryParam("player_token", formData.getFirst("player_token"))
        //     .queryParam("op_player_id", formData.getFirst("op_player_id"))
        //     .queryParam("operator", formData.getFirst("operator"));

        // URI uri = builder.build().encode().toUri();
        // BetDetailUrlVo responseVo = new BetDetailUrlVo();
        // responseVo.setUrl(uri.toString());
        // return responseVo;

        return new com.nextgen.gameaggregator.vendor.spribe.api.betdetail.BetDetailUrlVo();
    }
}
