package com.nextgen.gameaggregator.vendor.saba.api.balance;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.vendor.saba.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.saba.dto.RequestDto;
import com.nextgen.gameaggregator.vendor.saba.service.VendorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class GetBalanceAction {

    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorService vendorService;

    @PostMapping(path = EndPoints.GET_BALANCE)
    public GetBalanceVo action(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        // Construct Vo
        GetBalanceVo vo = new GetBalanceVo();

        try {
            // Convert original request body into dto
            RequestDto<GetBalanceDto> dto = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), new TypeReference<>() {});

            vo.setStatus("0");
            vo.setUserId(dto.getMessage().getUserId());
            vo.setBalance(BigDecimal.valueOf(1000));
            vo.setBalanceTs(vendorService.convertDateTimeFormat(System.currentTimeMillis()));

        } catch (Exception e) {
            System.out.println(e.getMessage());

        }

        return vo;
    }
}
