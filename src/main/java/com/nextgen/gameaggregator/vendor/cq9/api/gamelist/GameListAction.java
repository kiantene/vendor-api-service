package com.nextgen.gameaggregator.vendor.cq9.api.gamelist;

import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.cq9.constant.Credentials;
import com.nextgen.gameaggregator.vendor.cq9.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.cq9.vo.ResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(path = EndPoints.PATH, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
@Slf4j
public class GameListAction {

    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorLineService vendorLineService;

    @PostMapping(path = "/game/list")
    public ResponseVo<List<GameDetailsDto>> getGameListAction(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getTraceId();
        ResponseVo<List<GameDetailsDto>> responseVo = new ResponseVo<>();

        try {
            String apiUrl = vendorLineService.getCredentialValueByName(4, Credentials.API_URL);
            String apiToken = vendorLineService.getCredentialValueByName(4, Credentials.API_TOKEN);
            Optional.ofNullable(apiUrl).orElseThrow(InvalidVendorLineException::new);
            Optional.ofNullable(apiToken).orElseThrow(InvalidVendorLineException::new);

            ParameterizedTypeReference<ResponseVo<List<GameDetailsDto>>> responseType = new ParameterizedTypeReference<ResponseVo<List<GameDetailsDto>>>() {};

            responseVo = WebClient.create(apiUrl)
                    .get()
                    .uri(EndPoints.GAME_LIST, "cq9")
                    .header("Authorization", apiToken)
                    .retrieve()
                    .bodyToMono(responseType)
                    .block();

        } catch (Exception exception) {
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
    }
}
