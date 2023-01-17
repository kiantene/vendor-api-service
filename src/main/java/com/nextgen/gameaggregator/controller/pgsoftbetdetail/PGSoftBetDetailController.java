package com.nextgen.gameaggregator.controller.pgsoftbetdetail;

import com.nextgen.gameaggregator.entity.VendorLine;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.NoAvailableLineException;
import com.nextgen.gameaggregator.exception.VendorApiException;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.Credentials;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.pgsoft.vo.ResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping(path = "try/")
@Slf4j
public class PGSoftBetDetailController {

    @Autowired
    private VendorLineService vendorLineService;

//    @PostMapping(path = "betDetail")
//    public BetDetailResponseVo getGameList() {
//        try {
//            VendorLine vendorLine = vendorLineService.getVendorLineByAgent(4, 2, 2);
//            Map<String, String> lineCredentials = vendorLineService.toCredentialMap(vendorLine);
//
//            MultiValueMap<String,String> formData = formDataBuilder(vendorLine, lineCredentials);
//            String operatorSession = call(lineCredentials, formData);
//
//            System.out.println(betDetailResponseVo.toString());
//            return betDetailResponseVo;
//
//        } catch (InvalidVendorLineException invalidVendorLineException) {
//            throw new RuntimeException(invalidVendorLineException);
//
//        } catch (VendorApiException vendorApiException) {
//            throw new RuntimeException(vendorApiException);
//
//        } catch (NoAvailableLineException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//
//    public MultiValueMap<String, String> formDataBuilder(Map<String, String> credentials) throws InvalidVendorLineException {
//        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
//
//        String operatorToken = credentials.get(Credentials.OPERATOR_TOKEN);
//        Optional.ofNullable(operatorToken).orElseThrow(InvalidVendorLineException::new);
//
//        String secretKey = credentials.get(Credentials.SECRET_KEY);
//        Optional.ofNullable(secretKey).orElseThrow(InvalidVendorLineException::new);
//
//        formData.add("operator_token", operatorToken);
//        formData.add("secret_key", secretKey);
//
//        return formData;
//    }
//
//    public String obtainOperatorSession(Map<String, String> credentials) throws InvalidVendorLineException, VendorApiException {
//        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
//
//        String operatorToken = credentials.get(Credentials.OPERATOR_TOKEN);
//        Optional.ofNullable(operatorToken).orElseThrow(InvalidVendorLineException::new);
//
//        String secretKey = credentials.get(Credentials.SECRET_KEY);
//        Optional.ofNullable(secretKey).orElseThrow(InvalidVendorLineException::new);
//
//        formData.add("operator_token", operatorToken);
//        formData.add("secret_key", secretKey);
//
//        String apiUrl = credentials.get(Credentials.PGSOFT_API_DOMAIN);
//        Optional.ofNullable(apiUrl).orElseThrow(InvalidVendorLineException::new);
//
//        BetDetailResponseVo responseVo = WebClient.create(apiUrl)
//            .post()
//            .uri(Endpoints.BET_DETAIL_STEP_ONE)
//            .contentType(MediaType.APPLICATION_JSON)
//            .body(BodyInserters.fromFormData(formData))
//            .retrieve()
//            .bodyToMono(BetDetailResponseVo.class)
//            .block();
//
//        if (responseVo.getData() == null) {
//            throw new VendorApiException();
//        }
//
//        return responseVo.getData().getOperatorSession();
//    }
//
//    // return the bet detail url
//    public String call(MultiValueMap<String, String> formData, Map<String, String> credentials) throws InvalidVendorLineException, VendorApiException {
//
//        String operatorSession = obtainOperatorSession(lineCredentials, formData);
//        MultiValueMap<String,String> formData = formDataBuilder(vendorLine, lineCredentials);
//        return "";
//    }


}
