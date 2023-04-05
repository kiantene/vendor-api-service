package com.nextgen.gameaggregator.controller.pgsoftbetdetail;

import com.nextgen.gameaggregator.entity.Agent;
import com.nextgen.gameaggregator.entity.Vendor;
import com.nextgen.gameaggregator.entity.VendorLine;
import com.nextgen.gameaggregator.exception.InvalidVendorException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.NoAvailableLineException;
import com.nextgen.gameaggregator.exception.VendorApiException;
import com.nextgen.gameaggregator.repository.AgentRepository;
import com.nextgen.gameaggregator.repository.VendorRepository;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.Credentials;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.pgsoft.service.VendorService;
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
import java.util.UUID;

@RestController
@RequestMapping(path = "try/")
@Slf4j
public class PGSoftBetDetailController {

    @Autowired
    private VendorLineService vendorLineService;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @PostMapping(path = "betDetail")
    public String getGameList() {
        try {

            Agent agent = agentRepository.findAgentById(4);
            Vendor vendor = vendorRepository.findVendorById(2);

            VendorLine vendorLine = vendorLineService.getVendorLineByAgentAndGameCategory(agent, vendor, 2, 1);
            Map<String, String> lineCredentials = vendorLineService.toCredentialMap(vendorLine);

            String parentBetId = "1615259183702659072";
            String betId = "1615259183702659072";

            MultiValueMap<String,String> formData = formDataBuilder(parentBetId, betId, lineCredentials);
            String betDetailApi = call(formData, lineCredentials);

            return betDetailApi;

        } catch (InvalidVendorLineException invalidVendorLineException) {
            throw new RuntimeException(invalidVendorLineException);

        } catch (VendorApiException vendorApiException) {
            throw new RuntimeException(vendorApiException);

        } catch (NoAvailableLineException e) {
            throw new RuntimeException(e);
        } catch (InvalidVendorException e) {
            throw new RuntimeException(e);
        }
    }

    public MultiValueMap<String, String> formDataBuilder(String roundId, String externalBetId, Map<String, String> credentials) throws InvalidVendorLineException, VendorApiException {

        String operatorToken = credentials.get(Credentials.OPERATOR_TOKEN);
        Optional.ofNullable(operatorToken).orElseThrow(InvalidVendorLineException::new);

        String secretKey = credentials.get(Credentials.SECRET_KEY);
        Optional.ofNullable(secretKey).orElseThrow(InvalidVendorLineException::new);

        String apiUrl = credentials.get(Credentials.PGSOFT_API_DOMAIN);
        Optional.ofNullable(apiUrl).orElseThrow(InvalidVendorLineException::new);

        String operatorSession = obtainOperatorSession(operatorToken, secretKey, apiUrl);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("trace_id", String.valueOf(UUID.randomUUID()));
        formData.add("t", operatorSession); //operator session
        formData.add("psid", roundId); // parent bet id
        formData.add("sid", externalBetId); // bet id
        formData.add("lang", "en"); // en or zh
        formData.add("type", "operator");

        return formData;
    }

    public String obtainOperatorSession(String operatorToken, String secretKey, String apiUrl) throws VendorApiException {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        formData.add("operator_token", operatorToken);
        formData.add("secret_key", secretKey);

        BetDetailResponseVo responseVo = WebClient.create(apiUrl)
            .post()
            .uri(Endpoints.BET_DETAIL_STEP_ONE)
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromFormData(formData))
            .retrieve()
            .bodyToMono(BetDetailResponseVo.class)
            .block();

        if (responseVo.getData() == null) {
            throw new VendorApiException();
        }

        return responseVo.getData().getOperatorSession();
    }

    // return the bet detail url
    public String call(MultiValueMap<String, String> formData, Map<String, String> credentials) throws InvalidVendorLineException, VendorApiException {
        String apiUrl = credentials.get(Credentials.PUBLIC_DOMAIN);
        Optional.ofNullable(apiUrl).orElseThrow(InvalidVendorLineException::new);

        // https://public.pgsoft-games.com/history/redirect.html
        apiUrl = apiUrl + Endpoints.BET_DETAIL_STEP_TWO;

        String betDetailUrl = VendorService.generateBetDetailUrl(apiUrl, formData);

        return betDetailUrl;
    }

}
