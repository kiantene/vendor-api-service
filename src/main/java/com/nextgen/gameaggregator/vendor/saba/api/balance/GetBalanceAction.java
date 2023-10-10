package com.nextgen.gameaggregator.vendor.saba.api.balance;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.vendor.saba.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.saba.dto.RequestDto;
import com.nextgen.gameaggregator.vendor.saba.service.GzipUtils;
import com.nextgen.gameaggregator.vendor.saba.service.VendorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class GetBalanceAction {

    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorService vendorService;

    @PostMapping(path = EndPoints.GET_BALANCE)
    public GetBalanceVo action(@RequestBody byte[] request, HttpServletRequest httpServletRequest) throws IOException {

        Map<String, String> headers = new HashMap<>();

        Enumeration<String> headerNames = httpServletRequest.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String key = headerNames.nextElement();
            String value = httpServletRequest.getHeader(key);
            headers.put(key, value);
        }

        String decompressedRequestBody = GzipUtils.decompress(request);

        Map<String, String> requestLog = new HashMap<>();
        requestLog.put("Title", "SABA Testing");
        requestLog.put("Decompressed Request Body", decompressedRequestBody);
        requestLog.put("Request headers", headers.toString());
        requestLog.put("Request Input Stream", httpServletRequest.getInputStream().toString());
        log.info(requestLog.toString());

        String traceId = String.valueOf(UUID.randomUUID());

//        HttpRequestLog httpRequestLog = httpService.start(request);
//        String traceId = httpRequestLog.getId();

        // Construct Vo
        GetBalanceVo vo = new GetBalanceVo();

        try {
            // Convert original request body into dto
            RequestDto<GetBalanceDto> dto = HttpService.convertJsonToDto(decompressedRequestBody, new TypeReference<>() {
            });

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
