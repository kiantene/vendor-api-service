package com.nextgen.gameaggregator.vendor.pragmaticplayv2.api.endround;

import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.vendor.pragmaticplayv2.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.pragmaticplayv2.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;

@Component
@RequestMapping(path = Endpoints.PATH, consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
@Slf4j
public class EndRoundAction {
    private final HttpService httpService;

    @Autowired
    public EndRoundAction(HttpService httpService) {
        this.httpService = httpService;
    }

    public ResponseVo endRoundRequest(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        EndRoundVo responseVo = new EndRoundVo();

        responseVo.setCash(BigDecimal.ZERO);
        responseVo.setBonus(BigDecimal.ZERO);
        httpService.end(httpRequestLog, responseVo);
        return responseVo;
    }
}
