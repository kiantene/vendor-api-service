package com.nextgen.gameaggregator.vendor.saba.api.adjustment;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InsufficientBalanceException;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.sport.service.SportWalletService;
import com.nextgen.gameaggregator.vendor.saba.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.saba.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.saba.dto.RequestDto;
import com.nextgen.gameaggregator.vendor.saba.vo.GeneralVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class AdjustBalanceAction {

    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private HttpService httpService;
    @Autowired
    private SportWalletService sportWalletService;

    @PostMapping(path = EndPoints.ADJUST_BALANCE)
    public GeneralVo action(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        // Construct Vo
        GeneralVo vo = new GeneralVo();

        try {
            // Convert original request body into dto
            RequestDto<AdjustBalanceDto> dto = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), new TypeReference<>() {
            });

            sportWalletService.adjustment(traceId, dto.getMessage(), httpRequestLog);

            vo.setResponseCode(ResponseCode.SUCCESS);

        } catch (InsufficientBalanceException e) {
            vo.setResponseCode(ResponseCode.INSUFFICIENT_BALANCE);

        } catch (Exception e) {
            vo.setStatus("999");
            vo.setMsg("System Error");
            httpService.logError(httpRequestLog, e);

        } finally {
            httpService.end(httpRequestLog, vo);

        }

        return vo;

    }
}
