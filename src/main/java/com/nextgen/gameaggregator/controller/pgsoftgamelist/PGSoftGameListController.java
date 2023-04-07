package com.nextgen.gameaggregator.controller.pgsoftgamelist;

import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.exception.DisabledVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.operator.game.url.GameUrlService;
import com.nextgen.gameaggregator.repository.AgentRepository;
import com.nextgen.gameaggregator.repository.CurrencyRepository;
import com.nextgen.gameaggregator.repository.GameCategoryRepository;
import com.nextgen.gameaggregator.repository.VendorRepository;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.ValidationService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.Credentials;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.Endpoints;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping(path = "try/")
@Slf4j
public class PGSoftGameListController {
    @Autowired
    private HttpService httpService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private GameUrlService gameUrlService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private GameSessionService gameSessionService;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private CurrencyRepository currencyRepository;

    @Autowired
    private GameCategoryRepository gameCategoryRepository;

    @PostMapping(path = "gameList")
    public GameListResponseVo getGameList() {
        try {
            Agent agent = agentRepository.findAgentById(4);
            Vendor vendor = vendorRepository.findVendorById(2);
            Currency currency = currencyRepository.findByCode("CNY");
            GameCategory gameCategory = gameCategoryRepository.findByCode("SLOTS");

            VendorLine vendorLine = vendorLineService.findAgentVendorLine(agent, vendor, currency, gameCategory);
            Map<String, String> lineCredentials = vendorLineService.toCredentialMap(vendorLine);

            MultiValueMap<String,String> formData = formDataBuilder(vendorLine, lineCredentials);
            GameListResponseVo vo = call(formData, lineCredentials);

            return vo;

        } catch (InvalidVendorLineException e) {
            throw new RuntimeException(e);
        } catch ( DisabledVendorLineException e) {
            throw new RuntimeException(e);
        }
    }

    public MultiValueMap<String, String> formDataBuilder(VendorLine vendorLine, Map<String, String> credentials) throws InvalidVendorLineException {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        String operatorToken = credentials.get(Credentials.OPERATOR_TOKEN);
        Optional.ofNullable(operatorToken).orElseThrow(InvalidVendorLineException::new);

        String secretKey = credentials.get(Credentials.SECRET_KEY);
        Optional.ofNullable(secretKey).orElseThrow(InvalidVendorLineException::new);

        formData.add("operator_token", operatorToken);
        formData.add("secret_key", secretKey);
      //  formData.add("currency", vendorLine.getVendorCurrencyCode());
        formData.add("language", "zh-cn");
        formData.add("status", "1");
        return formData;
    }

    public GameListResponseVo call(MultiValueMap<String, String> formData, Map<String, String> credentials) throws InvalidVendorLineException {
        String apiUrl = credentials.get(Credentials.PGSOFT_API_DOMAIN);
        Optional.ofNullable(apiUrl).orElseThrow(InvalidVendorLineException::new);

        Integer megaBytes = 10;
        // WebClient with custom MaxInMemorySize (10MB)
        WebClient webClient = WebClient.builder().exchangeStrategies(ExchangeStrategies.builder()
            .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(megaBytes * 1024 * 1024))
            .build()).baseUrl(apiUrl).build();

        GameListResponseVo response = webClient
            .post()
            .uri(Endpoints.GAME_LIST)
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromFormData(formData))
            .retrieve()
            .bodyToMono(GameListResponseVo.class)
            .block();

        return response;
    }

}
