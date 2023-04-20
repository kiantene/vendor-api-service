package com.nextgen.gameaggregator.operator.game.terminate;

import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.operator.vo.OperatorResponseVo;
import com.nextgen.gameaggregator.service.HttpService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "game/")
@Slf4j
public class TerminateDto {

    @Autowired
    private HttpService httpService;

    @PostMapping(path = "terminate")
    public OperatorResponseVo list(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        OperatorResponseVo responseVo = new OperatorResponseVo<>();

        httpService.end(httpRequestLog, responseVo);
        return responseVo;
    }
}
