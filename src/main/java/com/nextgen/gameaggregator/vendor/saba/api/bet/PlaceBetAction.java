package com.nextgen.gameaggregator.vendor.saba.api.bet;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.vendor.saba.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.saba.dto.RequestDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class PlaceBetAction {

    @Autowired
    private HttpService httpService;

    @PostMapping(path = EndPoints.PLACE_BET)
    public PlaceBetVo action(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        // Construct Vo
        PlaceBetVo vo = new PlaceBetVo();

        try {
            // Convert original request body into dto
            RequestDto<PlaceBetDto> dto = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), new TypeReference<>() {
            });

            vo.setStatus("0");
            vo.setRefId(dto.getMessage().getRefId());
            vo.setLicenseeTxId(traceId);

        } catch (Exception e) {
            System.out.println(e.getMessage());

        } finally {
            httpService.end(httpRequestLog, vo);
        }

        return vo;
    }
}
