package com.nextgen.gameaggregator.vendor.bng.api.action;

import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.bng.constant.EndPoints;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class GeneralAction {

    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private HttpService httpService;

    @Autowired
    private VendorLineService vendorLineService;

    @PostMapping(path = EndPoints.ACTION)
    public LoginResponseDto action(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        LoginResponseDto responseDto = new LoginResponseDto();

        ArgsDto args = new ArgsDto();

        args.setPlatform("DESKTOP");

        responseDto.setName("login");
        responseDto.setUid("c22535914505424591bdaa930236932c");
        responseDto.setToken("TJj5ynZaLU");
        responseDto.setSession("400bd91815e94f06887c5ba61a332168");
        responseDto.setGame_id("151");
        responseDto.setGame_name("dragon_pearls_bng");
        responseDto.setProvider_id("1");
        responseDto.setProvider_name("booongo");
        responseDto.setC_at("2023-05-15T03:10:23+00:00");
        responseDto.setSent_at("2023-05-15T03:10:23+00:00");
        responseDto.setArgs(args);

        httpService.end(httpRequestLog, responseDto);

        return responseDto;
    }
}
