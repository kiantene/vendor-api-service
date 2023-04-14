package com.nextgen.gameaggregator.operator.transactions.detail;

import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.operator.vo.OperatorResponseVo;
import com.nextgen.gameaggregator.service.HttpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(path = "transactions/")
@Slf4j
public class TransactionDetailAction {

    @Autowired
    private HttpService httpService;

    @PostMapping(path = "detail")
    public OperatorResponseVo<TransactionDetailData> detail(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        OperatorResponseVo<TransactionDetailData> responseVo = new OperatorResponseVo<>();
        try {

        } finally {
            responseVo.setMessage(responseVo.getStatus().description);

        }
        httpService.end(httpRequestLog, responseVo);
        return responseVo;
    }
}
