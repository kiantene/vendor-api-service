package com.nextgen.gameaggregator.vendor.saba.api.resettle;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.sport.service.SportWalletService;
import com.nextgen.gameaggregator.vendor.saba.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.saba.dto.RequestDto;
import com.nextgen.gameaggregator.vendor.saba.vo.GeneralVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class ResettleAction {

    @Autowired
    private HttpService httpService;
    @Autowired
    private SportWalletService sportWalletService;

    @PostMapping(path = EndPoints.RESETTLE)
    public GeneralVo action(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        // Construct Vo
        GeneralVo vo = new GeneralVo();

        try {
            // Convert original request body into dto
            RequestDto<ResettleDto> dtos = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), new TypeReference<>() {
            });

            for (ResettleTransactionDto txn : dtos.getMessage().getTxns()) {
                sportWalletService.resettle(traceId, txn, httpRequestLog);
            }

            vo.setStatus("0");

        } catch (Exception e) {
            System.out.println(e.getMessage());

        } finally {
            httpService.end(httpRequestLog, vo);

        }

        return vo;
    }
}
