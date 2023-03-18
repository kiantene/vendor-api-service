package com.nextgen.gameaggregator.vendor.jdb.api.action;

import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.jdb.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.jdb.dto.VendorRequestDto;
import com.nextgen.gameaggregator.vendor.jdb.service.VendorService;
import com.nextgen.gameaggregator.vendor.jdb.vo.CommonVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class ActionClass {
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private HttpService httpService;

    @PostMapping(path = EndPoints.ACTION)
    public CommonVo action(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getTraceId();

        // Construct VO
        CommonVo vo = new CommonVo();

        try {
            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            // Convert original request body into dto
            VendorRequestDto commonDto = HttpService.convertQueryStringToDto(body, VendorRequestDto.class);
            ValidationUtils.validateRequest(commonDto);
            String params = VendorService.decrypt(commonDto.getX(), "47e0cd2ece0883e2", "b87f2867577b68ce");
            log.info(params);
            ActionDto actionDto = HttpService.convertJsonToDto(params, ActionDto.class);
            actionDto.setParams(params);

        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            log.error(ex.getMessage());
        } finally {
            httpService.end(httpRequestLog, vo);
        }

        vo.setBalance(BigDecimal.valueOf(1000));
        vo.setStatus("0000");
        vo.setErrText("Succeed");

        return vo;
    }
}
